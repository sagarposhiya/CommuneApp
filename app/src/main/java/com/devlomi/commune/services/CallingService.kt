package com.devlomi.commune.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.devlomi.commune.activities.calling.AgoraConfig
import com.devlomi.commune.activities.calling.CallingActivity
import com.devlomi.commune.activities.calling.event.CallingStateEvent
import com.devlomi.commune.activities.calling.event.CallingViewState
import com.devlomi.commune.activities.calling.model.*
import com.devlomi.commune.common.extensions.abandonAudioFocusCompat
import com.devlomi.commune.common.extensions.requestAudioFocusCompat
import com.devlomi.commune.model.constants.FireCallDirection
import com.devlomi.commune.model.realms.FireCall
import com.devlomi.commune.utils.*
import com.devlomi.commune.utils.network.CallsManager
import com.devlomi.commune.utils.network.FireManager
import io.agora.rtc.Constants
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoEncoderConfiguration
import io.agora.rtc.video.VideoEncoderConfiguration.FRAME_RATE
import io.agora.rtc.video.VideoEncoderConfiguration.VideoDimensions
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

class CallingService : Service(), ProximitySensor.Delegate, AudioManager.OnAudioFocusChangeListener, DuringCallEventHandler {
    private var callingState: CallingState = CallingState.NONE
    private var isCallActivityVisible = false
    private var isSpeakerEnabled = false
    private var isMicMuted = false
    private var isLocalVideoEnabled = false
    private var isCallActive = false
    private var isIncoming = false
    private val callingServiceInterface = CallingServiceInterface()

    var proximitySensor: ProximitySensor? = null
    var audioManager: AudioManager? = null
    var ringtonePlayer: RingtonePlayer? = null
    private var notificationId = -1
    private var hasAnswered = false

    private var timeObservable: Disposable? = null
    private var callTimeoutDeferDisposable: Disposable? = null

    var fireManager = FireManager()
    var channelName = ""
    private var callDuration = 0L


    private var fireCall: FireCall? = null

    private var videoUids = hashMapOf<Int, Boolean>()
    private val usersUids = hashMapOf<Int, Boolean>()
    private var isVideoCall = false
    private val callTimeoutDuration = 30L
    private val disposables = CompositeDisposable()
    private val callManager = CallsManager()
    private var audioFocusRequest: AudioFocusRequest? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        proximitySensor = ProximitySensor(this, this)
        ringtonePlayer = RingtonePlayer(this)
    }

    companion object {
        fun getStartIntent(context: Context, fireCall: FireCall, actionType: Int): Intent {
            return Intent(context, CallingService::class.java).apply {
                putExtra(IntentUtils.CALL_TYPE, CallType.fromInt(fireCall.callType))
                putExtra(IntentUtils.CALL_DIRECTION, fireCall.direction)
                putExtra(IntentUtils.CALL_ID, fireCall.callId)
                putExtra(IntentUtils.UID, fireCall.user.uid)
                putExtra(IntentUtils.PHONE, fireCall.phoneNumber)
                putExtra(IntentUtils.ISVIDEO, fireCall.isVideo)
                putExtra(IntentUtils.CALL_ACTION_TYPE, actionType)
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {


        if (intent != null) {
            if (intent.hasExtra(IntentUtils.CALL_ACTION_TYPE)) {
                val action = intent.getIntExtra(IntentUtils.CALL_ACTION_TYPE, -1)
                val callId = intent.getStringExtra(IntentUtils.CALL_ID)

                when (action) {
                    IntentUtils.NOTIFICATION_ACTION_START_INCOMING -> {

                        RealmHelper.getInstance().getFireCall(callId)?.let { fireCall ->

                            //prevent multiple incoming calls
                            if (MyApp.isIsCallActive()) return@let

                            MyApp.setCallActive(true)


                            this@CallingService.fireCall = fireCall
                            isIncoming = true
                            val uid = fireCall.user.uid
                            val callId = fireCall.callId
                            Intent(this@CallingService, CallingActivity::class.java).apply {
                                putExtra(IntentUtils.CALL_DIRECTION, FireCallDirection.INCOMING)
                                putExtra(IntentUtils.CALL_TYPE, fireCall.callType)
                                putExtra(IntentUtils.CALL_ID, callId)
                                putExtra(IntentUtils.UID, uid)
                                putExtra(IntentUtils.PHONE, fireCall.phoneNumber)
                                putExtra(IntentUtils.CALL_ACTION_TYPE, IntentUtils.NOTIFICATION_ACTION_START_INCOMING)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                this@CallingService.startActivity(this)
                            }

                            val incomingCallNotification = NotificationHelper(this@CallingService).createIncomingCallNotification(fireCall, getNotificationId())
                            startForeground(getNotificationId(), incomingCallNotification)
                            if (ringtonePlayer != null && audioManager!!.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                                requestAudioFocus(AudioManager.STREAM_VOICE_CALL)
                                ringtonePlayer!!.playIncomingRingtone()
                            }

                            startDefer()

                            if (!fireCall.isGroupCall) {

                                callManager.listenForEndingCall(fireCall.callId, fireCall.user.uid, isIncoming).subscribe({
                                    if (it.exists()) {
                                        endCall(CallEndedReason.REMOTE_REJECTED)
                                    }
                                }, { error ->

                                }).addTo(disposables)
                            }
                        }

                    }

                    IntentUtils.NOTIFICATION_ACTION_DECLINE -> {
                        rejectCall()
                    }
                }
            }
        }
        return START_NOT_STICKY
    }


    override fun onDestroy() {
        stop()
        super.onDestroy()
    }

    private fun setCallEnded(callId: String, otherUid: String, isIncoming: Boolean) {
        fireCall?.let { fireCall ->
            if (!fireCall.isGroupCall) {
                ServiceHelper.setCallEnded(this, callId, otherUid, isIncoming)
            }
        }
    }

    @SuppressLint("NewApi")
    private fun stop() {


        videoUids.clear()
        usersUids.clear()
        setCallingState(CallingState.NONE)
        isLocalVideoEnabled = false


        leaveChannel()

        setCallActive(false)
        ringtonePlayer?.stopRingtone()

        setBluetoothHeadset(false)
        audioManager?.abandonAudioFocusCompat(this, audioFocusRequest)

        stopListenForSensor()

        fireCall?.let { call ->

            RealmHelper.getInstance().updateCallInfoOnCallEnded(call.callId, callDuration.toInt())

        }

        NotificationHelper(this@CallingService).cancelIncomingCallNotification()
        notificationId = -1

        application().removeEventHandler(this@CallingService)
        timeObservable?.dispose()
        callTimeoutDeferDisposable?.dispose()
        timeObservable = null
        callTimeoutDeferDisposable = null
        callDuration = 0

        disposables.dispose()


        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(IntentUtils.ACTION_FINISH_CALLING_ACTIVITY))
        stopForeground(true)

        stopSelf()


    }

    private fun setCallActive(mCallActive: Boolean) {
        isCallActive = mCallActive
        MyApp.setCallActive(mCallActive)
    }


    override fun onBind(intent: Intent): IBinder? {
        return callingServiceInterface
    }


    private fun requestAudioFocus(streamType: Int) {
        audioFocusRequest = audioManager?.requestAudioFocusCompat(this, streamType, AudioManager.AUDIOFOCUS_GAIN)
    }

    private fun startForeground() {
        setCallActive(true)
        val streamType = AudioManager.STREAM_VOICE_CALL

        fireCall?.let { fireCall ->
            val activeCallNotification = NotificationHelper(this).createActiveCallNotification(fireCall, getNotificationId())
            startForeground(getNotificationId(), activeCallNotification)
            RealmHelper.getInstance().saveObjectToRealm(fireCall)
            requestAudioFocus(streamType)
        }
    }


    private fun setBluetoothHeadset(enable: Boolean) {
        if (enable) {
            audioManager?.mode = AudioManager.MODE_NORMAL
            audioManager?.isBluetoothScoOn = true
            audioManager?.startBluetoothSco()
            audioManager?.mode = AudioManager.MODE_IN_CALL
        } else {
            audioManager?.isBluetoothScoOn = false
            audioManager?.stopBluetoothSco()
            audioManager?.mode = AudioManager.MODE_NORMAL
        }
    }


    override fun onProximitySensorNear() {
        if (isCallActivityVisible) proximitySensor?.acquire()
    }

    override fun onProximitySensorFar() {
        proximitySensor?.release()
    }


    private fun setCallingState(callingState: CallingState) {
        this.callingState = callingState
        postViewEvent(CallingViewState.UpdateCallingState(callingState))
    }


    private fun onCallEstablished() {
        callTimeoutDeferDisposable?.dispose()
        postViewEvent(CallingViewState.OnCallEstablished)
        if (timeObservable == null) {
            timeObservable = Observable.interval(1, TimeUnit.SECONDS).subscribe({ interval ->
                postViewEvent(CallingViewState.UpdateDuration(interval))
            }, { error ->

            }).addTo(disposables)
        }

        ringtonePlayer?.stopRingtone()


        //divert sound to bluetooh headset if available
        if (AudioHelper.isBluetoothHeadsetOn(audioManager))
            setBluetoothHeadset(true)

        setCallActive(true)

        fireCall?.let { call ->
            if (call.direction == FireCallDirection.INCOMING) {
                if (call.isGroupCall) {
                    callManager.setCallAnsweredForGroup(call.callId, call.user.uid)
                } else {
                    callManager.setCallAnswered(call.callId, call.user.uid, true)
                }
                RealmHelper.getInstance().setCallAsAnswered(call.callId)
                val notificationHelper = NotificationHelper(this)
                val activeCallNotification = notificationHelper.createActiveCallNotification(call, getNotificationId())
                notificationHelper.notifyNotification(getNotificationId(), activeCallNotification)
            }
        }
    }


    override fun onExtraCallback(type: Int, data: Array<Any?>) {
        Handler(Looper.getMainLooper()).post {

            when (type) {

                AGEventHandler.EVENT_TYPE_ON_USER_VIDEO_MUTED -> {

                    val peerUid = data[0] as Int
                    val muted = data[1] as Boolean

                    postViewEvent(CallingViewState.MuteOrUnmuteRemoteViewForUid(peerUid, muted))
                }

                AGEventHandler.EVENT_TYPE_CONNECTION_STATE_CHANGED -> {
                    val state = data[0] as? Int
                    val reason = data[1] as? Int

                    state?.let { connectionState ->

                        when (connectionState) {
                            Constants.CONNECTION_STATE_CONNECTING -> {
                                setCallingState(CallingState.CONNECTING)
                            }
                            Constants.CONNECTION_STATE_CONNECTED -> {
                                setCallingState(CallingState.CONNECTED)
                            }
                            Constants.CONNECTION_STATE_FAILED -> {
                                setCallingState(CallingState.FAILED)
                            }

                            Constants.CONNECTION_STATE_RECONNECTING -> {
                                setCallingState(CallingState.RECONNECTING)
                            }
                        }
                    }

                }
            }
        }
    }


    override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
        Handler(Looper.getMainLooper()).post {

            postViewEvent(CallingViewState.JoinChannelSuccess(uid))
        }
    }

    override fun onUserJoined(uid: Int) {

        Handler(Looper.getMainLooper()).post {
            hasAnswered = true
            //check if this user joins for the first time
            if (usersUids.isEmpty()) {
                setCallingState(CallingState.ANSWERED)
                onCallEstablished()
            }

            usersUids[uid] = true
        }

    }

    override fun onDecodingRemoteVideo(uid: Int, elapsed: Int) {
        Handler(Looper.getMainLooper()).post {

            videoUids[uid] = false
            postViewEvent(CallingViewState.SetupRemoteViewForUid(uid))
        }
    }


    override fun onUserOffline(uid: Int, reason: Int) {

        Handler(Looper.getMainLooper()).post {

            videoUids.remove(uid)
            usersUids.remove(uid)
            postViewEvent(CallingViewState.RemoveRemoteViewForUid(uid))
            if (videoUids.isEmpty() || areAllUsersMuted()) {
                postViewEvent(CallingViewState.HideRemoteViews)
            }

            if (usersUids.isEmpty()) {
                if (fireCall?.isGroupCall == false) {
                    endCall(CallEndedReason.REMOTE_HUNG_UP)
                }
            }
        }
    }

    private fun areAllUsersMuted(): Boolean = videoUids.values.all { it }


    fun endCall(reason: CallEndedReason) {
        postViewEvent(CallingViewState.CallEnded(reason))
        stop()
    }

    fun setSpeakerEnabled(isEnabled: Boolean) {
        isSpeakerEnabled = isEnabled
        rtcEngine().setEnableSpeakerphone(isSpeakerEnabled)

        if (isEnabled) {
            stopListenForSensor()
        } else {
            startListenForSensor()
        }
    }

    override fun onAudioFocusChange(i: Int) {}

    private fun doConfigEngine() {
        val videoDimension = AgoraConfig.videoDimension
        val videoFps = AgoraConfig.videoFps
        configEngine(videoDimension, videoFps, null, null)
    }

    private fun joinChannel(channelName: String, uid: Int): Int {
        this@CallingService.channelName = channelName
        val result = rtcEngine().joinChannel(null, channelName, null, uid)

        //SUCCESS
        if (result == 0) {
            config().mChannel = channelName
        }

        return result
    }


    private fun startDefer() {
        if (callTimeoutDeferDisposable == null) {
            callTimeoutDeferDisposable = Completable.complete()
                    .delay(callTimeoutDuration, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        fireCall?.let { fireCall ->
                            setCallEnded(fireCall.callId, fireCall.user.uid, isIncoming)
                        }

                        endCall(CallEndedReason.NO_ANSWER)
                    }, { error ->

                    }).addTo(disposables)
        }


    }


    private fun leaveChannel() {
        config().mChannel = null
        rtcEngine().leaveChannel()
        config().reset()
    }

    fun postViewEvent(viewEvent: CallingViewState) {
        EventBus.getDefault().post(viewEvent)
    }

    private fun configEngine(videoDimension: VideoDimensions?, fps: FRAME_RATE?, encryptionKey: String?, encryptionMode: String?) {


        // Set the Resolution, FPS. Bitrate and Orientation of the video
        rtcEngine().setVideoEncoderConfiguration(VideoEncoderConfiguration(videoDimension,
                fps,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT))
    }


    private fun stopListenForSensor() {
        proximitySensor?.stopListenForSensor()
        proximitySensor?.release()
    }

    private fun application(): MyApp {
        return application as MyApp
    }

    private fun rtcEngine(): RtcEngine {
        return application().rtcEngine()
    }

    private fun config(): EngineConfig {
        return application().config()
    }


    private fun startListenForSensor() {
        if (fireCall?.isVideo == true) return

        if (!AudioHelper.isHeadsetOn(audioManager) && !isSpeakerEnabled) {
            proximitySensor?.listenForSensor()
        }
    }

    private fun getNotificationId(): Int {
        if (notificationId == -1) notificationId = NotificationHelper.generateId()
        return notificationId
    }


    fun startCall(fireCall: FireCall, isIncoming: Boolean) {

        MyApp.setCallActive(true)
        this@CallingService.fireCall = fireCall
        this@CallingService.isIncoming = isIncoming

        setCallingState(CallingState.INITIATING)
        application().addEventHandler(this@CallingService)

        this@CallingService.channelName = fireCall.channel
        if (fireCall.isVideo) {
            isLocalVideoEnabled = true
            isVideoCall = true
            doConfigEngine()
            rtcEngine().enableVideo()
            postViewEvent(CallingViewState.SetupLocalView)
        } else {
            rtcEngine().disableVideo()
        }


        isSpeakerEnabled = fireCall.isVideo

        startDefer()
        val joinChannelResult = joinChannel(channelName, config().mUid)

        startForeground()
        if (!isIncoming) {
            ringtonePlayer?.playProgressTone()
        }
        //if Success
        if (joinChannelResult == 0) {
            if (!isIncoming) {

                if (fireCall.isGroupCall) {
                    callManager.saveOutgoingGroupCallOnFirebase(fireCall, fireCall.user.uid).subscribe({

                    }, { error ->
                        endCall(CallEndedReason.ERROR)
                    }).addTo(disposables)

                } else {

                    callManager.saveOutgoingCallOnFirebase(fireCall, fireCall.user.uid).subscribe({
                    }, { error ->
                        endCall(CallEndedReason.ERROR)
                    }).addTo(disposables)


                    callManager.listenForEndingCall(fireCall.callId, fireCall.user.uid, isIncoming).subscribe({
                        if (it.exists()) {
                            endCall(CallEndedReason.REMOTE_HUNG_UP)
                        }
                    }, { error ->

                    }).addTo(disposables)
                }

            }


        } else {
            endCall(CallEndedReason.ERROR)
        }


    }


    private fun rejectCall() {
        fireCall?.let { fireCall ->
            postViewEvent(CallingViewState.CallEnded(CallEndedReason.LOCAL_REJECTED))


            if (fireCall.isGroupCall) {
                ServiceHelper.setCallDeclinedForGroup(this, fireCall.callId, fireCall.user.uid)
            } else {
                setCallEnded(fireCall.callId, fireCall.user.uid, isIncoming)
            }
        }
        stop()
    }

    inner class CallingServiceInterface : Binder() {


        fun setStateEvent(stateEvent: CallingStateEvent) {

            when (stateEvent) {

                is CallingStateEvent.StartCall -> {
                    startCall(stateEvent.fireCall, stateEvent.isIncoming)
                }


                is CallingStateEvent.SpeakerClicked -> {
                    setSpeakerEnabled(!isSpeakerEnabled)
                    if (isSpeakerEnabled) {
                        postViewEvent(CallingViewState.EnableSpeaker)
                    } else {
                        postViewEvent(CallingViewState.DisableSpeaker)
                    }
                }

                is CallingStateEvent.BtnVideoClicked -> {
                    isLocalVideoEnabled = !isLocalVideoEnabled
                    rtcEngine().muteLocalVideoStream(!isLocalVideoEnabled)
                    if (isLocalVideoEnabled) {
                        postViewEvent(CallingViewState.ResumeLocalVideo)
                    } else {
                        postViewEvent(CallingViewState.PauseLocalVideo)
                    }

                }

                is CallingStateEvent.MicClicked -> {
                    isMicMuted = !isMicMuted
                    rtcEngine().muteLocalAudioStream(isMicMuted)
                    postViewEvent(CallingViewState.MicMuted(isMicMuted))
                }

                is CallingStateEvent.FlipCameraClicked -> {
                    rtcEngine().switchCamera()
                }

                is CallingStateEvent.EndCall -> {
                    fireCall?.let { fireCall ->
                        setCallEnded(fireCall.callId, fireCall.user.uid, isIncoming)
                    }
                    endCall(CallEndedReason.LOCAL_HUNG_UP)
                }

                is CallingStateEvent.OnStart -> {
                    MyApp.phoneCallActivityResumed()

                    if (isVideoCall) {
                        videoUids.keys.forEach { uid ->

                            //check if it's not muted
                            videoUids[uid]?.let { isMuted ->
                                if (!isMuted) {
                                    rtcEngine().muteRemoteVideoStream(uid, false)
                                }

                                //update non-synced remote views while on Stop
                                postViewEvent(CallingViewState.SetupRemoteViewForUid(uid))

                                postViewEvent(CallingViewState.MuteOrUnmuteRemoteViewForUid(uid, isMuted))

                            }
                        }
                    }

                }

                is CallingStateEvent.OnStop -> {
                    MyApp.phoneCallActivityPaused()
                    if (isVideoCall) {
                        videoUids.keys.forEach { uid ->

                            //check if it's not muted
                            videoUids[uid]?.let { isMuted ->
                                if (!isMuted) {
                                    rtcEngine().muteRemoteVideoStream(uid, true)
                                }
                            }

                        }
                    }

                }

                is CallingStateEvent.OnWindowFocusChanged -> {
                    val visible = stateEvent.hasFocus
                    isCallActivityVisible = visible
                    if (visible) {
                        startListenForSensor()
                    } else {
                        stopListenForSensor()
                    }
                }


                is CallingStateEvent.AnswerIncoming -> {
                    fireCall?.let { fireCall ->
                        startCall(fireCall, true)
                    }
                }

                is CallingStateEvent.RejectIncoming -> {
                    rejectCall()
                }
                is CallingStateEvent.VolumeKeyPressed -> {
                    if (!isIncoming) {
                        ringtonePlayer?.stopRingtone()
                    }
                }

                is CallingStateEvent.UpdateMe -> {
                    postViewEvent(CallingViewState.UpdateCallingState(callingState))

                    if (isIncoming && hasAnswered) {
                        postViewEvent(CallingViewState.HideAnswerButtons)
                    }

                    if (isLocalVideoEnabled) {
                        postViewEvent(CallingViewState.SetupLocalView)
                        postViewEvent(CallingViewState.ResumeLocalVideo)
                    } else {
                        postViewEvent(CallingViewState.PauseLocalVideo)
                    }

                    if (isSpeakerEnabled) {
                        postViewEvent(CallingViewState.EnableSpeaker)
                    } else {
                        postViewEvent(CallingViewState.DisableSpeaker)
                    }

                    postViewEvent(CallingViewState.MicMuted(isMicMuted))

                    videoUids.keys.forEach { uid ->

                        if (videoUids[uid] == false) {
                            postViewEvent(CallingViewState.SetupRemoteViewForUid(uid))
                            postViewEvent(CallingViewState.MuteOrUnmuteRemoteViewForUid(uid, false))
                        } else {
                            postViewEvent(CallingViewState.MuteOrUnmuteRemoteViewForUid(uid, true))
                        }

                    }


                }
            }
        }


    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stop()
    }

}

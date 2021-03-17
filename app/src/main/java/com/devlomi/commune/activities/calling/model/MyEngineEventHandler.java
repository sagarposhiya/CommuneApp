/*
 * Created by Devlomi on 2020
 */

package com.devlomi.commune.activities.calling.model;


import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;

import static io.agora.rtc.Constants.REMOTE_VIDEO_STATE_DECODING;
import static io.agora.rtc.Constants.REMOTE_VIDEO_STATE_REASON_REMOTE_UNMUTED;
import static io.agora.rtc.Constants.REMOTE_VIDEO_STATE_STOPPED;

public class MyEngineEventHandler extends IRtcEngineEventHandler {

    public static final int NO_CONNECTION_ERROR = 3;


    private final ConcurrentHashMap<AGEventHandler, Integer> mEventHandlerList = new ConcurrentHashMap<>();

    public void addEventHandler(AGEventHandler handler) {
        this.mEventHandlerList.put(handler, 0);
    }

    public void removeEventHandler(AGEventHandler handler) {
        this.mEventHandlerList.remove(handler);
    }


    @Override
    public void onConnectionStateChanged(int state, int reason) {
        super.onConnectionStateChanged(state, reason);
        Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
        while (it.hasNext()) {
            AGEventHandler handler = it.next();
            if (handler instanceof DuringCallEventHandler) {
                ((DuringCallEventHandler) handler).onExtraCallback(AGEventHandler.EVENT_TYPE_CONNECTION_STATE_CHANGED, new Object[]{state, reason});
            }
        }
    }


    /**
     * Occurs when the first local video frame is rendered.
     * This callback is triggered after the first local video frame is rendered on the local video window.
     *
     * @param width   Width (pixels) of the first local video frame.
     * @param height  Height (pixels) of the first local video frame.
     * @param elapsed Time elapsed (ms) from the local user calling joinChannel until this callback is triggered. If startPreview is called before joinChannel, elapsed is the time elapsed (ms) from the local user calling startPreview until this callback is triggered.
     */
    @Override
    public void onFirstLocalVideoFrame(int width, int height, int elapsed) {

    }


    /**
     * Occurs when a remote user (Communication)/host (Live Broadcast) joins the channel.
     * <p>
     * Communication profile: This callback notifies the app when another user joins the channel. If other users are already in the channel, the SDK also reports to the app on the existing users.
     * Live Broadcast profile: This callback notifies the app when the host joins the channel. If other hosts are already in the channel, the SDK also reports to the app on the existing hosts. We recommend having at most 17 hosts in a channel
     * <p>
     * The SDK triggers this callback under one of the following circumstances:
     * <p>
     * A remote user/host joins the channel by calling the joinChannel method.
     * A remote user switches the user role to the host by calling the setClientRole method after joining the channel.
     * A remote user/host rejoins the channel after a network interruption.
     * The host injects an online media stream into the channel by calling the addInjectStreamUrl method.
     *
     * @param uid     ID of the user or host who joins the channel.
     * @param elapsed Time delay (ms) from the local user calling joinChannel/setClientRole until this callback is triggered.
     */
    @Override
    public void onUserJoined(int uid, int elapsed) {

        Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
        while (it.hasNext()) {
            AGEventHandler handler = it.next();
            if (handler instanceof DuringCallEventHandler) {
                ((DuringCallEventHandler) handler).onUserJoined(uid);
            }
        }
    }

    /**
     * Occurs when a remote user (Communication)/host (Live Broadcast) leaves the channel.
     * <p>
     * There are two reasons for users to become offline:
     * <p>
     * Leave the channel: When the user/host leaves the channel, the user/host sends a goodbye message. When this message is received, the SDK determines that the user/host leaves the channel.
     * Drop offline: When no data packet of the user or host is received for a certain period of time (20 seconds for the communication profile, and more for the live broadcast profile), the SDK assumes that the user/host drops offline. A poor network connection may lead to false detections, so we recommend using the Agora RTM SDK for reliable offline detection.
     *
     * @param uid    ID of the user or host who leaves the channel or goes offline.
     * @param reason Reason why the user goes offline:
     *               <p>
     *               USER_OFFLINE_QUIT(0): The user left the current channel.
     *               USER_OFFLINE_DROPPED(1): The SDK timed out and the user dropped offline because no data packet was received within a certain period of time. If a user quits the call and the message is not passed to the SDK (due to an unreliable channel), the SDK assumes the user dropped offline.
     *               USER_OFFLINE_BECOME_AUDIENCE(2): (Live broadcast only.) The client role switched from the host to the audience.
     */
    @Override
    public void onUserOffline(int uid, int reason) {

        Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
        while (it.hasNext()) {
            AGEventHandler handler = it.next();
            if (handler instanceof DuringCallEventHandler) {
                ((DuringCallEventHandler) handler).onUserOffline(uid, reason);
            }
        }
    }


    /**
     * Reports the statistics of the RtcEngine once every two seconds.
     *
     * @param stats RTC engine statistics: RtcStats.
     */
    @Override
    public void onRtcStats(RtcStats stats) {
    }

    /**
     * Reports the statistics of the video stream from each remote user/host. The SDK triggers this callback once every two seconds for each remote user/host. If a channel includes multiple remote users, the SDK triggers this callback as many times.
     *
     * @param stats of the received remote video streams: RemoteVideoStats.
     */
    @Override
    public void onRemoteVideoStats(RemoteVideoStats stats) {

        Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
        while (it.hasNext()) {
            AGEventHandler handler = it.next();
            if (handler instanceof DuringCallEventHandler) {
                ((DuringCallEventHandler) handler).onExtraCallback(AGEventHandler.EVENT_TYPE_ON_USER_VIDEO_STATS, new Object[]{stats});
            }
        }
    }

    @Override
    public void onRemoteVideoStateChanged(int uid, int state, int reason, int elapsed) {
        super.onRemoteVideoStateChanged(uid, state, reason, elapsed);

        Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
        while (it.hasNext()) {
            AGEventHandler handler = it.next();
            if (handler instanceof DuringCallEventHandler) {

                switch (state) {
                    case REMOTE_VIDEO_STATE_STOPPED:

                        ((DuringCallEventHandler) handler).onExtraCallback(AGEventHandler.EVENT_TYPE_ON_USER_VIDEO_MUTED, new Object[]{uid, true});
                        break;
                    case REMOTE_VIDEO_STATE_DECODING:
                        if (reason == REMOTE_VIDEO_STATE_REASON_REMOTE_UNMUTED){
                            ((DuringCallEventHandler) handler).onExtraCallback(AGEventHandler.EVENT_TYPE_ON_USER_VIDEO_MUTED, new Object[]{uid, false});
                        }else {
                            ((DuringCallEventHandler) handler).onDecodingRemoteVideo(uid, elapsed);
                        }
                        break;



                }
            }
        }
    }


    /**
     * Reports which users are speaking and the speakers' volume, and whether the local user is speaking.
     * <p>
     * This callback reports the IDs and volumes of the loudest speakers (at most 3) at the moment in the channel, and whether the local user is speaking.
     * <p>
     * By default, this callback is disabled. You can enable it by calling the enableAudioVolumeIndication method. Once enabled, this callback is triggered at the set interval, regardless of whether a user speaks or not.
     * <p>
     * The SDK triggers two independent onAudioVolumeIndication callbacks at one time, which separately report the volume information of the local user and all the remote speakers. For more information, see the detailed parameter descriptions.
     *
     * @param speakerInfos An array containing the user ID and volume information for each speaker: AudioVolumeInfo.
     *                     <p>
     *                     In the local user’s callback, this array contains the following members:
     *                     uid = 0,
     *                     volume = totalVolume, which reports the sum of the voice volume and audio-mixing volume of the local user, and
     *                     vad, which reports the voice activity status of the local user.
     *                     <p>
     *                     In the remote speakers' callback, this array contains the following members:
     *                     uid of each remote speaker,
     *                     volume, which reports the sum of the voice volume and audio-mixing volume of each remote speaker, and
     *                     vad = 0.
     *                     <p>
     *                     An empty speakers array in the callback indicates that no remote user is speaking at the moment
     * @param totalVolume  Total volume after audio mixing. The value ranges between 0 (lowest volume) and 255 (highest volume).
     *                     <p>
     *                     In the local user’s callback, totalVolume is the sum of the voice volume and audio-mixing volume of the local user.
     *                     In the remote speakers' callback, totalVolume is the sum of the voice volume and audio-mixing volume of all remote speakers.
     */
    @Override
    public void onAudioVolumeIndication(AudioVolumeInfo[] speakerInfos, int totalVolume) {
        if (speakerInfos == null) {
            return;
        }

        Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
        while (it.hasNext()) {
            AGEventHandler handler = it.next();
            if (handler instanceof DuringCallEventHandler) {
                ((DuringCallEventHandler) handler).onExtraCallback(AGEventHandler.EVENT_TYPE_ON_SPEAKER_STATS, new Object[]{(Object) speakerInfos});
            }
        }
    }

    /**
     * Occurs when a user leaves the channel.
     * <p>
     * When the app calls the leaveChannel method, the SDK uses this callback to notify the app when the user leaves the channel.
     * <p>
     * With this callback, the application retrieves the channel information, such as the call duration and statistics.
     *
     * @param stats Statistics of the call: RtcStats
     */
    @Override
    public void onLeaveChannel(RtcStats stats) {

    }

    /**
     * Reports the last mile network quality of the local user once every two seconds before the user joins the channel. Last mile refers to the connection between the local device and Agora's edge server. After the application calls the enableLastmileTest method, this callback reports once every two seconds the uplink and downlink last mile network conditions of the local user before the user joins the channel.
     *
     * @param quality The last mile network quality based on the uplink and dowlink packet loss rate and jitter:
     *                <p>
     *                QUALITY_UNKNOWN(0): The quality is unknown.
     *                QUALITY_EXCELLENT(1): The quality is excellent.
     *                QUALITY_GOOD(2): The quality is quite good, but the bitrate may be slightly lower than excellent.
     *                QUALITY_POOR(3): Users can feel the communication slightly impaired.
     *                QUALITY_BAD(4): Users can communicate not very smoothly.
     *                QUALITY_VBAD(5): The quality is so bad that users can barely communicate.
     *                QUALITY_DOWN(6): The network is disconnected and users cannot communicate at all.
     *                QUALITY_DETECTING(8): The SDK is detecting the network quality.
     */
    @Override
    public void onLastmileQuality(int quality) {
        Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
        while (it.hasNext()) {
            AGEventHandler handler = it.next();
            if (handler instanceof BeforeCallEventHandler) {
                ((BeforeCallEventHandler) handler).onLastmileQuality(quality);
            }
        }
    }

    /**
     * Reports the last-mile network probe result.
     * <p>
     * Since
     * v2.4.0.
     * <p>
     * The SDK triggers this callback within 30 seconds after the app calls the startLastmileProbeTest method.
     *
     * @param result The uplink and downlink last-mile network probe test result. For details, see LastmileProbeResult.
     */
    @Override
    public void onLastmileProbeResult(LastmileProbeResult result) {
        Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
        while (it.hasNext()) {
            AGEventHandler handler = it.next();
            if (handler instanceof BeforeCallEventHandler) {
                ((BeforeCallEventHandler) handler).onLastmileProbeResult(result);
            }
        }
    }


    /**
     * Reports an error during SDK runtime.
     * <p>
     * In most cases, the SDK cannot fix the issue and resume running. The SDK requires the app to take action or informs the user about the issue.
     * <p>
     * For example, the SDK reports an ERR_START_CALL error when failing to initialize a call. The app informs the user that the call initialization failed and invokes the leaveChannel method to leave the channel. For detailed error codes, see Error Codes.
     *
     * @param error Error Code
     */
    @Override
    public void onError(int error) {

        Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
        while (it.hasNext()) {
            AGEventHandler handler = it.next();
            if (handler instanceof DuringCallEventHandler) {
                ((DuringCallEventHandler) handler).onExtraCallback(AGEventHandler.EVENT_TYPE_ON_AGORA_MEDIA_ERROR, new Object[]{error, RtcEngine.getErrorDescription(error)});
            }
        }
    }

    /**
     * Occurs when the local user receives a remote data stream.
     * <p>
     * The SDK triggers this callback when the local user receives the stream message that the remote user sends by calling the sendStreamMessage method.
     *
     * @param uid      User ID of the remote user sending the data stream.
     * @param streamId Stream ID.
     * @param data     Data received by the local user.
     */
    @Override
    public void onStreamMessage(int uid, int streamId, byte[] data) {

        Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
        while (it.hasNext()) {
            AGEventHandler handler = it.next();
            if (handler instanceof DuringCallEventHandler) {
                ((DuringCallEventHandler) handler).onExtraCallback(AGEventHandler.EVENT_TYPE_ON_DATA_CHANNEL_MSG, new Object[]{uid, data});
            }
        }
    }

    /**
     * Occurs when the local user fails to receive a remote data stream.
     * <p>
     * The SDK triggers this callback when the local user fails to receive the stream message that the remote user sends by calling the sendStreamMessage method.
     *
     * @param uid      User ID of the remote user sending the data stream.
     * @param streamId Stream ID.
     * @param error    Error Code.
     * @param missed   The number of lost messages.
     * @param cached   The number of incoming cached messages when the data stream is interrupted.
     */
    public void onStreamMessageError(int uid, int streamId, int error, int missed, int cached) {

        Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
        while (it.hasNext()) {
            AGEventHandler handler = it.next();
            if (handler instanceof DuringCallEventHandler) {
                ((DuringCallEventHandler) handler).onExtraCallback(AGEventHandler.EVENT_TYPE_ON_AGORA_MEDIA_ERROR, new Object[]{error, "on stream msg error " + (uid & 0xFFFFFFFFL) + " " + missed + " " + cached});
            }
        }
    }

    /**
     * Occurs when the SDK cannot reconnect to Agora's edge server 10 seconds after its connection to the server is interrupted.
     * The SDK triggers this callback when it cannot connect to the server 10 seconds after calling joinChannel(), regardless of whether it is in the channel or not.
     * If the SDK fails to rejoin the channel 20 minutes after being disconnected from Agora's edge server, the SDK stops rejoining the channel.
     */
    @Override
    public void onConnectionLost() {

        Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
        while (it.hasNext()) {
            AGEventHandler handler = it.next();
            if (handler instanceof DuringCallEventHandler) {
                ((DuringCallEventHandler) handler).onExtraCallback(AGEventHandler.EVENT_TYPE_ON_APP_ERROR, new Object[]{NO_CONNECTION_ERROR});
            }
        }
    }


    /**
     * Occurs when the local user joins a specified channel.
     * <p>
     * The channel name assignment is based on channelName specified in the joinChannel method.
     * <p>
     * If the uid is not specified when joinChannel is called, the server automatically assigns a uid.
     *
     * @param channel Channel name.
     * @param uid     User ID.
     * @param elapsed Time elapsed (ms) from the user calling joinChannel until this callback is triggered.
     */
    @Override
    public void onJoinChannelSuccess(String channel, int uid, int elapsed) {

        Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
        while (it.hasNext()) {
            AGEventHandler handler = it.next();
            if (handler instanceof DuringCallEventHandler) {
                ((DuringCallEventHandler) handler).onJoinChannelSuccess(channel, uid, elapsed);
            }
        }
    }

    /**
     * Occurs when the local audio playback route changes.
     * This callback returns that the audio route switched to an earpiece, speakerphone, headset, or Bluetooth device.
     *
     * @param routing The definition of the routing is listed as follows:
     *                <p>
     *                AUDIO_ROUTE_DEFAULT(-1): Default audio route.
     *                AUDIO_ROUTE_HEADSET(0): Headset.
     *                AUDIO_ROUTE_EARPIECE(1): Earpiece.
     *                AUDIO_ROUTE_HEADSETNOMIC(2): Headset with no microphone.
     *                AUDIO_ROUTE_SPEAKERPHONE(3): Speakerphone.
     *                AUDIO_ROUTE_LOUDSPEAKER(4): Loudspeaker.
     *                AUDIO_ROUTE_HEADSETBLUETOOTH(5): Bluetooth headset.
     */
    @Override
    public void onAudioRouteChanged(int routing) {

        Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
        while (it.hasNext()) {
            AGEventHandler handler = it.next();
            if (handler instanceof DuringCallEventHandler) {
                ((DuringCallEventHandler) handler).onExtraCallback(AGEventHandler.EVENT_TYPE_ON_AUDIO_ROUTE_CHANGED, new Object[]{routing});
            }
        }
    }

    /**
     * Reports a warning during SDK runtime.
     * <p>
     * In most cases, the app can ignore the warning reported by the SDK because the SDK can usually fix the issue and resume running.
     * <p>
     * For instance, the SDK may report a WARN_LOOKUP_CHANNEL_TIMEOUT warning upon disconnection with the server and tries to reconnect. For detailed warning codes, see Warning Codes.
     *
     * @param warn Warning Code
     */
    public void onWarning(int warn) {

        String msg = "Check io.agora.rtc.Constants for details";
        Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
        while (it.hasNext()) {
            AGEventHandler handler = it.next();
            if (handler instanceof DuringCallEventHandler) {
                ((DuringCallEventHandler) handler).onExtraCallback(AGEventHandler.EVENT_TYPE_ON_AGORA_MEDIA_ERROR, new Object[]{warn, msg});
            }
        }
    }

    /**
     * Occurs when the state of the local user's audio mixing file changes.
     * <p>
     * Since
     * v2.4.0. When you call the startAudioMixing method and the state of audio mixing file changes, the Agora SDK triggers this callback.
     * <p>
     * When the audio mixing file plays, pauses playing, or stops playing, this callback returns 710, 711, or 713 in state, and 0 in errorCode.
     * When exceptions occur during playback, this callback returns 714 in state and an error in errorCode.
     * If the local audio mixing file does not exist, or if the SDK does not support the file format or cannot access the music file URL, the SDK returns WARN_AUDIO_MIXING_OPEN_ERROR = 701.
     *
     * @param state     The state code:
     *                  <p>
     *                  MEDIA_ENGINE_AUDIO_EVENT_MIXING_PLAY(710): the audio mixing file is playing.
     *                  MEDIA_ENGINE_AUDIO_EVENT_MIXING_PAUSED(711): the audio mixing file pauses playing.
     *                  MEDIA_ENGINE_AUDIO_EVENT_MIXING_STOPPED(713): the audio mixing file stops playing.
     *                  MEDIA_ENGINE_AUDIO_EVENT_MIXING_ERROR(714): an exception occurs when playing the audio mixing file. See the errorCode for details.
     * @param errorCode The error code:
     *                  <p>
     *                  MEDIA_ENGINE_AUDIO_ERROR_MIXING_OPEN(701): the SDK cannot open the audio mixing file.
     *                  MEDIA_ENGINE_AUDIO_ERROR_MIXING_TOO_FREQUENT(702): the SDK opens the audio mixing file too frequently.
     *                  MEDIA_ENGINE_AUDIO_EVENT_MIXING_INTERRUPTED_EOF(703): the audio mixing file playback is interrupted.
     */
    @Override
    public void onAudioMixingStateChanged(int state, int errorCode) {
    }
}

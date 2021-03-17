package com.devlomi.commune.utils

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.devlomi.commune.R
import com.devlomi.commune.activities.calling.CallingActivity
import com.devlomi.commune.activities.calling.model.CallType
import com.devlomi.commune.model.constants.FireCallDirection
import com.devlomi.commune.utils.network.FireManager
import com.devlomi.commune.utils.network.FireManager.Companion.generateKey
import io.reactivex.disposables.CompositeDisposable

class PerformCall(var context: Activity, var fireManager: FireManager, var disposables: CompositeDisposable) {

    //this will check for call requirements then open the Calling Activity
    fun performCall(isVideo: Boolean, uid: String?) {
        if (!NetworkHelper.isConnected(context)) {
            Toast.makeText(context, R.string.no_internet_connection, Toast.LENGTH_SHORT).show()
            return
        }
        if (MyApp.isIsCallActive()) {
            Toast.makeText(context, R.string.there_is_active_call_currently, Toast.LENGTH_SHORT).show()
            return
        }
        if (isVideo && !PermissionsUtil.hasVideoCallPermissions(context)) {
            Toast.makeText(context, R.string.missing_permissions, Toast.LENGTH_SHORT).show()
            return
        } else if (!isVideo && !PermissionsUtil.hasVoiceCallPermissions(context)) {
            Toast.makeText(context, R.string.missing_permissions, Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = AlertDialog.Builder(context)
        val message = if (isVideo) R.string.video_call_confirmation else R.string.voice_call_confirmation
        dialog.setMessage(message)
        dialog.setNegativeButton(R.string.no, null)
                .setPositiveButton(R.string.yes) { dialogInterface, i2 ->
                    val progressDialog = ProgressDialog(context)
                    progressDialog.setMessage(context.resources.getString(R.string.loading))
                    progressDialog.show()
                    disposables.add(fireManager.isUserBlocked(uid!!).subscribe({ isBlocked: Boolean ->
                        progressDialog.dismiss()
                        if (isBlocked) {
                            Util.showSnackbar(context, context.resources.getString(R.string.error_calling))
                        } else {
                            val callType = if (isVideo) CallType.VIDEO else CallType.VOICE
                            val callScreen = Intent(context, CallingActivity::class.java)
                            callScreen.putExtra(IntentUtils.CALL_TYPE, callType.value)
                            callScreen.putExtra(IntentUtils.CALL_DIRECTION, FireCallDirection.OUTGOING)
                            callScreen.putExtra(IntentUtils.UID, uid)
                            callScreen.putExtra(IntentUtils.CALL_ID, generateKey())
                            callScreen.putExtra(IntentUtils.CALL_ACTION_TYPE, IntentUtils.ACTION_START_NEW_CALL)
                            context.startActivity(callScreen)

                        }
                    }) { throwable: Throwable? -> progressDialog.dismiss() })
                }
        dialog.show()
    }

    //this will check for call requirements then open the Calling Activity
    fun performConferenceCall(isVideo: Boolean, groupId: String?) {
        if (!NetworkHelper.isConnected(context)) {
            Toast.makeText(context, R.string.no_internet_connection, Toast.LENGTH_SHORT).show()
            return
        }
        if (MyApp.isIsCallActive()) {
            Toast.makeText(context, R.string.there_is_active_call_currently, Toast.LENGTH_SHORT).show()
            return
        }
        if (isVideo && !PermissionsUtil.hasVideoCallPermissions(context)) {
            Toast.makeText(context, R.string.missing_permissions, Toast.LENGTH_SHORT).show()
            return
        } else if (!isVideo && !PermissionsUtil.hasVoiceCallPermissions(context)) {
            Toast.makeText(context, R.string.missing_permissions, Toast.LENGTH_SHORT).show()
            return
        }
        val dialog = AlertDialog.Builder(context)
        val message = if (isVideo) R.string.video_call_confirmation else R.string.voice_call_confirmation
        dialog.setMessage(message)
        dialog.setNegativeButton(R.string.no, null)
                .setPositiveButton(R.string.yes) { dialogInterface, i2 ->
                    val callType = if (isVideo) CallType.CONFERENCE_VIDEO else CallType.CONFERENCE_VOICE
                    val callScreen = Intent(context, CallingActivity::class.java)
                    callScreen.putExtra(IntentUtils.CALL_TYPE, callType.value)
                    callScreen.putExtra(IntentUtils.CALL_DIRECTION, FireCallDirection.OUTGOING)
                    callScreen.putExtra(IntentUtils.UID, groupId)
                    callScreen.putExtra(IntentUtils.CALL_ID, generateKey())
                    callScreen.putExtra(IntentUtils.CALL_ACTION_TYPE, IntentUtils.ACTION_START_NEW_CALL)
                    context.startActivity(callScreen)
                }
        dialog.show()
    }
}
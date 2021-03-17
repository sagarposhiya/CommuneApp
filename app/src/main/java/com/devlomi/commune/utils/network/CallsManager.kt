/*
 * Created by Devlomi on 2020
 */

package com.devlomi.commune.utils.network

import com.devlomi.commune.extensions.observeValueEvent
import com.devlomi.commune.extensions.setValueRx
import com.devlomi.commune.model.realms.FireCall
import com.devlomi.commune.utils.FireConstants
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ServerValue
import io.reactivex.Completable
import io.reactivex.Flowable

class CallsManager {

    companion object {
        const val CALL_TIEMOUT_SECONDS = 40

    }

    fun saveOutgoingCallOnFirebase(fireCall: FireCall, otherUid: String): Completable {
        return FireConstants.newCallsRef.child(otherUid).child(FireManager.uid).child(fireCall.callId).setValueRx(fireCall.toMap())
    }

    fun saveOutgoingGroupCallOnFirebase(fireCall: FireCall, groupId: String): Completable {
        val map = mutableMapOf<String, Any>().apply {
            this["timestamp"] = ServerValue.TIMESTAMP
            this["callType"] = fireCall.callType
            this["callId"] = fireCall.callId
            this["groupId"] = groupId
            this["callerId"] = FireManager.uid
            this["channel"] = fireCall.channel
        }
        return FireConstants.groupCallsRef.child(groupId).child(fireCall.callId).setValueRx(map)
    }


    //this will reject/decline/hangup a call
    fun setCallEnded(callId: String, otherUid: String, isIncoming: Boolean): Completable {
        return if (isIncoming) {
            FireConstants.newCallsRef.child(FireManager.uid).child(otherUid).child(callId).child("ended_incoming").setValueRx(true)
        } else {
            FireConstants.newCallsRef.child(otherUid).child(FireManager.uid).child(callId).child("ended_outgoing").setValueRx(true)
        }
    }

    //this will reject/decline/hangup a call
    fun setCallAnsweredForGroup(callId: String, groupId: String): Completable {
      return FireConstants.groupCallsRef.child(groupId).child(callId).child("answered").child(FireManager.uid).setValueRx(FireManager.uid)
    }

    //this will reject/decline/hangup a call
    fun setCallRejectedForGroup(callId: String, groupId: String): Completable {
        return FireConstants.groupCallsRef.child(groupId).child(callId).child("declined").child(FireManager.uid).setValueRx(FireManager.uid)
    }

    //this will reject/decline/hangup a call
    fun setCallAnswered(callId: String, otherUid: String, isIncoming: Boolean): Completable {
        return if (isIncoming) {
            FireConstants.newCallsRef.child(FireManager.uid).child(otherUid).child(callId).child("hasAnswered").setValueRx(true)
        } else {
            FireConstants.newCallsRef.child(otherUid).child(FireManager.uid).child(callId).child("hasAnswered").setValueRx(true)
        }
    }

    fun listenForEndingCall(callId: String, otherUid: String, isIncoming: Boolean): Flowable<DataSnapshot> {
        return if (isIncoming) {
            FireConstants.newCallsRef.child(FireManager.uid).child(otherUid).child(callId).child("ended_outgoing").observeValueEvent()
        } else {
            FireConstants.newCallsRef.child(otherUid).child(FireManager.uid).child(callId).child("ended_incoming").observeValueEvent()
        }
    }

}
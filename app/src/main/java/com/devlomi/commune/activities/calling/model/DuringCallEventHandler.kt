/*
 * Created by Devlomi on 2020
 */

package com.devlomi.commune.activities.calling.model

interface DuringCallEventHandler : AGEventHandler {
    fun onUserJoined(uid: Int)
    fun onDecodingRemoteVideo(uid: Int,  elapsed: Int)
    fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int)
    fun onUserOffline(uid: Int, reason: Int)
    fun onExtraCallback(type: Int,  data: Array<Any?>)
}
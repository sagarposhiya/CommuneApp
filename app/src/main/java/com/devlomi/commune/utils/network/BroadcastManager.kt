package com.devlomi.commune.utils.network

import com.devlomi.commune.extensions.observeSingleValueEvent
import com.devlomi.commune.extensions.setValueRx
import com.devlomi.commune.extensions.updateChildrenRx
import com.devlomi.commune.model.constants.DBConstants
import com.devlomi.commune.model.realms.Broadcast
import com.devlomi.commune.model.realms.User
import com.devlomi.commune.utils.FireConstants
import com.devlomi.commune.utils.RealmHelper
import com.devlomi.commune.utils.SharedPreferencesManager

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ServerValue
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.realm.RealmList
import java.util.*

class BroadcastManager {
    //this will create a new group and add users to this group

    fun createNewBroadcast(broadcastName: String, selectedUsers: List<User>): Single<User> {
        //generate broadcastId
        val broadcastId = FireConstants.broadcastsRef.push().key!!
        //generate temp group image
        val result: MutableMap<String, Any> = HashMap()
        val broadcastInfo: MutableMap<String, Any> = HashMap()
        //set timeCreated
        broadcastInfo[DBConstants.TIMESTAMP] = ServerValue.TIMESTAMP
        //set whom created the group
        broadcastInfo["createdBy"] = SharedPreferencesManager.getPhoneNumber()
        //set onlyAdminsCanPost by default to false
        val usersMap = User.toMap(selectedUsers, true)
        broadcastInfo["name"] = broadcastName
        result["info"] = broadcastInfo
        result["users"] = usersMap
        return Single.create { emitter ->
            FireConstants.broadcastsRef.child(broadcastId).setValueRx(result).subscribe({
                val broadcastUser = createBroadcastLocally(broadcastName, selectedUsers, broadcastId, Date().time)
                emitter.onSuccess(broadcastUser)
            }, {
                emitter.onError(it)
            })
        }


    }

    private fun createBroadcastLocally(broadcastName: String, selectedUsers: List<User>, broadcastId: String, timestamp: Long): User {
        val broadcastUser = User()
        broadcastUser.userName = broadcastName
        broadcastUser.status = ""
        broadcastUser.phone = ""
        val list = RealmList<User>()
        list.addAll(selectedUsers)
        val broadcast = Broadcast()
        broadcast.broadcastId = broadcastId
        broadcast.users = list
        broadcast.timestamp = timestamp
        broadcast.createdByNumber = SharedPreferencesManager.getPhoneNumber()
        broadcastUser.broadcast = broadcast
        broadcastUser.isBroadcastBool = true
        broadcastUser.uid = broadcastId
        RealmHelper.getInstance().saveObjectToRealm(broadcastUser)
        RealmHelper.getInstance().saveEmptyChat(broadcastUser)
        return broadcastUser
    }


    fun deleteBroadcast(broadcastId: String): Completable {
        return FireConstants.broadcastsRef.child(broadcastId!!).setValueRx(null).doOnComplete {
            RealmHelper.getInstance().deleteBroadcast(broadcastId)
        }
    }


    fun removeBroadcastMember(broadcastId: String, userToDeleteUid: String): Completable {
        return FireConstants.broadcastsRef.child(broadcastId).child("users").child(userToDeleteUid).setValueRx(null).doOnComplete {
            RealmHelper.getInstance().deleteBroadcastMember(broadcastId, userToDeleteUid)
        }
    }


    fun addParticipant(broadcastId: String, selectedUsers: ArrayList<User>): Completable {
        val map: MutableMap<String, Any> = HashMap()

        for (selectedUser in selectedUsers) {
            map[selectedUser.uid] = false
        }
        return FireConstants.broadcastsRef.child(broadcastId).child("users").updateChildrenRx(map).doOnComplete {
            for (selectedUser in selectedUsers) {
                RealmHelper.getInstance().addUserToBroadcast(broadcastId, selectedUser)
            }
        }
    }


    fun changeBroadcastName(broadcastId: String, newTitle: String): Completable {
        return FireConstants.broadcastsRef.child(broadcastId).child("info").child("name").setValueRx(newTitle).doOnComplete {
            RealmHelper.getInstance().changeBroadcastName(broadcastId, newTitle)
        }
    }

    fun fetchBroadcast(broadcastId: String): Observable<User> {
        //get only broadcasts that created by this user
        return FireConstants.broadcastsRef.child(broadcastId).observeSingleValueEvent().flatMapObservable { dataSnapshot ->


            val info = dataSnapshot.child("info")
            val usersSnapshot = dataSnapshot.child("users")
            val broadcastUserIds = getBroadcastUsersIds(usersSnapshot).filterNotNull()

            return@flatMapObservable UserByIdsDataSource.getUsersByIds(broadcastUserIds).map { Pair(it, info) }
        }.map { pair ->
            val users = pair.first
            val info = pair.second
            val broadcastName = info.child("name").getValue(String::class.java) ?: ""
            val timestampVal = info.child("timestamp").getValue(Long::class.java)

            val timestamp = timestampVal ?: Date().time

            return@map createBroadcastLocally(broadcastName, users, broadcastId, timestamp)

        }
    }


    private fun getBroadcastUsersIds(usersSnapshot: DataSnapshot): List<String?> {
        val uids: MutableList<String?> = ArrayList()
        for (child in usersSnapshot.children) {
            val uid = child.key
            //don't add current user to broadcast
            if (uid != FireManager.uid) {
                uids.add(uid)
            }
        }
        return uids
    }


    fun fetchBroadcasts(uid: String): Observable<List<User>> {
        //get only broadcasts that created by this user
        return FireConstants.broadcastsByUser.child(uid).orderByValue().equalTo(true).observeSingleValueEvent().map { dataSnapshot ->
            if (dataSnapshot.hasChildren().not()) {
                return@map listOf<String>()
            } else {
                return@map dataSnapshot.children.map { it.key!! }
            }

        }.flatMapObservable { broadcastsIds ->

            val observablesList = broadcastsIds.map { fetchBroadcast(it) }
            return@flatMapObservable Observable.merge(observablesList).toList().toObservable()

        }
    }


}
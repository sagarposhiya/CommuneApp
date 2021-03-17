package com.devlomi.commune.utils.network

import android.net.Uri
import com.devlomi.commune.extensions.*
import com.devlomi.commune.model.constants.DBConstants
import com.devlomi.commune.model.constants.GroupEventTypes
import com.devlomi.commune.model.constants.GroupEventTypes.GROUP_CREATION
import com.devlomi.commune.model.constants.GroupEventTypes.USER_ADDED
import com.devlomi.commune.model.realms.Group
import com.devlomi.commune.model.realms.GroupEvent
import com.devlomi.commune.model.realms.User
import com.devlomi.commune.utils.*
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ktx.getValue
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import durdinapps.rxfirebase2.RxFirebaseDatabase
import durdinapps.rxfirebase2.RxFirebaseStorage
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.realm.RealmList
import java.io.File
import java.util.*

class GroupManager {

    private fun saveAndCreateNewGroup(groupId: String, groupTitle: String, thumbImg: String,
                                      photoUrl: String, users: List<User>, adminUids: List<String>,
                                      timestamp: Long, createdBy: String, onlyAdminsCanPost: Boolean,
                                      isCreatedByThisUser: Boolean): User {
        val groupUser = User()
        groupUser.userName = groupTitle
        groupUser.photo = photoUrl
        groupUser.thumbImg = thumbImg
        val list = RealmList<User>()
        for (user in users) {
            list.add(user)
        }
        val currentUser = SharedPreferencesManager.getCurrentUser()
        list.add(currentUser)
        val group = Group()

        val adminUidsList = RealmList<String>()
        adminUidsList.addAll(adminUids)
        group.adminsUids = adminUidsList


        group.groupId = groupId
        group.isActive = true
        group.setUsers(list)
        group.timestamp = timestamp
        group.createdByNumber = createdBy
        group.isOnlyAdminsCanPost = onlyAdminsCanPost

        groupUser.group = group
        groupUser.isGroupBool = true
        groupUser.uid = groupId
        RealmHelper.getInstance().saveObjectToRealm(groupUser)



        if (isCreatedByThisUser) {

            val groupEvent = GroupEvent()
            groupEvent.contextStart = FireManager.phoneNumber!!
            groupEvent.eventType = GroupEventTypes.GROUP_CREATION

            groupEvent.createGroupEvent(groupUser, null)
            //add Group events 'this user added user x'
            for (user in list) {
                if (user.uid != FireManager.uid) {
                    val groupEvent = GroupEvent()
                    groupEvent.contextStart = FireManager.phoneNumber!!
                    groupEvent.eventType = USER_ADDED
                    groupEvent.contextEnd = user.phone
                    groupEvent.createGroupEvent(groupUser, null)
                }
            }
        }

        return groupUser

    }

    fun fetchGroupPartialInfo(groupId: String): Observable<Pair<User, Int>> {
        return FireConstants.groupsRef.child(groupId).observeSingleValueEvent().flatMapObservable { dataSnapshot ->
            val info = dataSnapshot.child("info")
            val usersSnapshot = dataSnapshot.child("users")

            val uids = usersSnapshot.children.take(6).map { it.key }.filterNotNull()

            return@flatMapObservable UserByIdsDataSource.getUsersByIds(uids).map { Triple(it, info, usersSnapshot) }


        }.map {
            val users = it.first
            val infoSnapshot = it.second
            val usersSnapshot = it.third

            //group details
            val groupName = infoSnapshot.child("name").getValue(String::class.java)
            val photo = infoSnapshot.child("photo").getValue(String::class.java)
            val createdBy = infoSnapshot.child("createdBy").getValue(String::class.java)
            val usersInGroupCount = usersSnapshot.childrenCount.toInt()

            //NOTE this is un-managed object(Not saved to Database)
            val userGroup = User()
            userGroup.userName = groupName
            userGroup.photo = photo
            val group = Group()
            group.groupId = groupId
            group.createdByNumber = createdBy
            val userList = RealmList<User>()
            userList.addAll(users)
            group.setUsers(userList)
            userGroup.group = group

            return@map Pair(userGroup, usersInGroupCount)
        }
    }


    fun fetchAndCreateGroup(groupId: String): Observable<User> {

        return RxFirebaseDatabase.observeSingleValueEvent(FireConstants.groupsRef.child(groupId)).toObservable()
                .flatMap { snapshot ->

                    val usersSnapshot = snapshot.child("users")


                    val usersUids = usersSnapshot.children.map { it }.map { it.key!! }



                    return@flatMap UserByIdsDataSource.getUsersByIds(usersUids).map { Pair(it, snapshot) }

                }.map {
                    val users = it.first
                    val snapshot = it.second
                    val info = snapshot.child("info")
                    val usersSnapshot = snapshot.child("users")


                    val groupName = info.child("name").value as? String ?: ""
                    val photo = info.child("photo").value as? String ?: ""
                    val thumbImg = info.child("thumbImg").value as? String ?: ""
                    val createdBy = info.child("createdBy").value as? String ?: ""
                    val createdAtTimestamp = info.child("timestamp").value as? Long ?: 0
                    val onlyAdminsCanPost = info.child("onlyAdminsCanPost").value as? Boolean
                            ?: false

                    var adminUids = mutableListOf<String>()
                    for (snapshot in usersSnapshot.children) {
                        val isAdmin = snapshot.value as? Boolean
                        isAdmin?.let {
                            adminUids.add(snapshot.key!!)
                        }

                    }
                    return@map saveAndCreateNewGroup(groupId, groupName, thumbImg,
                            photo, users!!, adminUids, createdAtTimestamp, createdBy,
                            onlyAdminsCanPost, false)

                }.flatMap { groupUser ->
                    return@flatMap FirebaseMessaging.getInstance().subscribeToTopicRx(groupId).andThen(Observable.just(groupUser))
                }.flatMap { groupUser ->
                    val query = FireConstants.groupsEventsRef.child(groupId).limitToLast(10)

                    return@flatMap RxFirebaseDatabase.observeSingleValueEvent(query).toObservable().map { Pair(it, groupUser) }
                }.map {
                    val snapshot = it.first
                    val groupUser = it.second
                    if (snapshot.exists()) {
                        for (snap in snapshot.children) {


                            val groupEvent = snap.getValue<GroupEvent>()
                            groupEvent?.let { groupEventNotNullable ->
                                //if it's a creation event
                                if (groupEventNotNullable.contextStart == groupEventNotNullable.contextEnd) {
                                    groupEvent.eventType = GROUP_CREATION
                                    groupEvent.contextEnd = "null"
                                }
                                groupEvent.createGroupEvent(groupUser, groupEvent.eventId)

                            }
                        }

                    }
                    return@map groupUser
                }
    }

    fun createNewGroup(groupTitle: String, users: List<User>): Single<User> {
        val context = MyApp.context()
        val groupId = FireConstants.groupsRef.push().key!!
        val photoFile = File(context.cacheDir, "group-img.png")
        val ref = FireConstants.mainRef.child("defaultGroupProfilePhoto")
        return RxFirebaseDatabase.observeSingleValueEvent(ref).flatMap { snapshot ->
            val photo = snapshot.value as String
            val referenceFromUrl = FirebaseStorage.getInstance().getReferenceFromUrl(photo)
            return@flatMap RxFirebaseStorage.getFile(referenceFromUrl, photoFile).toMaybe().map { Pair(it, photo) }
        }.map {
            val task = it.first
            val photoUrl = it.second
            var result = mutableMapOf<String, Any>()
            var groupInfo = mutableMapOf<String, Any>()

            val circleBitmap = BitmapUtils.getCircleBitmap(BitmapUtils.convertFileImageToBitmap(photoFile.path))
            val thumbImg = BitmapUtils.decodeImageAsPng(circleBitmap)



            groupInfo[DBConstants.TIMESTAMP] = ServerValue.TIMESTAMP
            groupInfo["createdBy"] = FireManager.phoneNumber!!
            groupInfo["onlyAdminsCanPost"] = false
            val usersDict = User.toMap(users, true)

            groupInfo["name"] = groupTitle

            groupInfo["photo"] = photoUrl
            groupInfo["thumbImg"] = thumbImg
            result["info"] = groupInfo
            result["users"] = usersDict


            return@map Triple(photoUrl, thumbImg, result)
        }.flatMapSingle {
            val photoUrl = it.first
            val thumbImg = it.second
            val map = it.third

            val pair = Pair(photoUrl, thumbImg)
            return@flatMapSingle FireConstants.groupsRef.child(groupId).setValueRx(map).toSingleDefault(it).map { pair }

        }.flatMap {
            val photoUrl = it.first
            val thumbImg = it.second

            val groupUser = saveAndCreateNewGroup(groupId, groupTitle, thumbImg, photoUrl, users,
                    listOf(FireManager.uid), Date().time, FireManager.phoneNumber!!,
                    false, isCreatedByThisUser = true);



            return@flatMap Single.just(groupUser)
        }

    }


    fun joinViaGroupLink(groupId: String): Completable {
        return fetchAndCreateGroup(groupId).flatMapCompletable {
            return@flatMapCompletable RxFirebaseDatabase.setValue(FireConstants.groupsRef.child(groupId).child("users").child(FireManager.uid), false)
        }
    }

    fun getGroupIdByGroupLink(groupLink: String): Observable<String> {

        return RxFirebaseDatabase.observeSingleValueEvent(FireConstants.groupsLinks.child(groupLink)).toObservable().flatMap { snapshot ->

            val groupId = snapshot.value as? String

            if (groupId != null) {
                return@flatMap Observable.just(groupId)
            }

            return@flatMap Observable.error<String>(Throwable("Invalid Group Link"))
        }


    }

    fun removeGroupMember(groupId: String, uidOfUserToRemove: String): Completable {
        return RxFirebaseDatabase.setValue(FireConstants.groupsRef.child(groupId).child("users").child(uidOfUserToRemove), null)
    }

    fun addParticipant(groupId: String, selectedUsers: ArrayList<User>): Completable {
        val usersMap = User.toMap(selectedUsers, false)

        return RxFirebaseDatabase.updateChildren(FireConstants.groupsRef.child(groupId).child("users"), usersMap)
    }

    fun changeGroupName(groupTitle: String, groupId: String): Completable {
        return RxFirebaseDatabase.setValue(FireConstants.groupsRef.child(groupId).child("info").child("name"), groupTitle).doOnComplete {
            RealmHelper.getInstance().changeGroupName(groupId, groupTitle)
        }
    }

    //this will upload the user photo that he picked and generate a Small circle image and decode as base64
    fun changeGroupImage(imagePath: String, groupId: String): Completable {
        //generate new name for the file when uploading to firebase storage
        val fileName = UUID.randomUUID().toString() + Util.getFileExtensionFromPath(imagePath)
        //upload image
        val ref = FireConstants.imageProfileRef.child(fileName)

        val file = Uri.fromFile(File(imagePath))



        return ref.putFileRx(file).flatMapMaybe {
            return@flatMapMaybe RxFirebaseStorage.getDownloadUrl(ref)
        }.flatMapCompletable { downloadUrl ->
            val updateMap: MutableMap<String, Any> = HashMap()

            //generate circle bitmap
            val circleBitmap = BitmapUtils.getCircleBitmap(BitmapUtils.convertFileImageToBitmap(imagePath))
            //decode the image as base64 string
            val decodedImage = BitmapUtils.decodeImageAsPng(circleBitmap)

            //add the photo to the map
            updateMap["photo"] = downloadUrl.toString()
            //add the thumb circle image to the map
            updateMap["thumbImg"] = decodedImage

            //save them in firebase database using one request
            return@flatMapCompletable RxFirebaseDatabase.updateChildren(FireConstants.groupsRef.child(groupId).child("info"), updateMap)
        }
    }

    fun exitGroup(groupId: String, uid: String): Completable {

        return FirebaseMessaging.getInstance().unsubscribeFromTopicRx(groupId).andThen(
                RxFirebaseDatabase.setValue(FireConstants.groupsRef.child(groupId).child("users").child(uid), null)
        )

    }

    //this will update group info if something is changed,whether it's users change or group info change
    fun updateGroup(groupId: String, groupEvent: GroupEvent?): Observable<MutableList<User>> {

        return RxFirebaseDatabase.observeSingleValueEvent(FireConstants.groupsRef.child(groupId)).flatMapObservable { dataSnapshot ->
            val info = dataSnapshot.child("info")
            val users = dataSnapshot.child("users")
            val unfetchedUsers = RealmHelper.getInstance().updateGroup(groupId, info, users)
                    ?: return@flatMapObservable Observable.empty<MutableList<User>>()


            if (groupEvent != null) {

                //if it is a creation event show whom created this group event
                val mGroupEvent: GroupEvent = if (groupEvent.contextStart == groupEvent.contextEnd) {
                    GroupEvent(groupEvent.contextStart, GroupEventTypes.GROUP_CREATION, "null")
                } else {
                    GroupEvent(groupEvent.contextStart, groupEvent.eventType, groupEvent.contextEnd)
                }
                val group = RealmHelper.getInstance().getUser(groupId)
                        ?: return@flatMapObservable Observable.empty<MutableList<User>>()
                mGroupEvent.createGroupEvent(group, mGroupEvent.eventId)
            }

            if (unfetchedUsers.isNotEmpty()) {

                return@flatMapObservable UserByIdsDataSource.getUsersByIds(unfetchedUsers)


            } else {
                RealmHelper.getInstance().deletePendingGroupCreationJob(groupId)
                return@flatMapObservable Observable.empty<MutableList<User>>()
            }
        }.doOnNext { users ->
            for (user in users) {
                RealmHelper.getInstance().addUsersToGroup(groupId, user)
                RealmHelper.getInstance().deletePendingGroupCreationJob(groupId)
            }
        }
    }


    fun fetchUserGroups(): Observable<List<User>> {

        return RxFirebaseDatabase.observeSingleValueEvent(FireConstants.groupsByUser.child(FireManager.uid)).flatMapObservable { snapshot ->
            val groupsIds = snapshot.children.map { it.key }
            val observablesList = groupsIds.map { fetchAndCreateGroup(it!!) }
            return@flatMapObservable Observable.merge(observablesList).toList().toObservable()
        }

    }

    fun isUserBannedFromGroup(groupId: String, userId: String): Single<Boolean> {
        return FireConstants.deletedGroupsUsers.child(groupId).child(userId).snapshotAtRefExists()
    }


}


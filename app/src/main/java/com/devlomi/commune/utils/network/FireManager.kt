package com.devlomi.commune.utils.network

import android.content.Context
import android.net.Uri
import com.devlomi.commune.extensions.getFileRx
import com.devlomi.commune.extensions.observeSingleValueEvent
import com.devlomi.commune.extensions.setValueRx
import com.devlomi.commune.extensions.snapshotAtRefExists
import com.devlomi.commune.model.ImageItem
import com.devlomi.commune.model.constants.LastSeenStates
import com.devlomi.commune.model.constants.MessageStat
import com.devlomi.commune.model.constants.MessageType
import com.devlomi.commune.model.realms.User
import com.devlomi.commune.utils.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ktx.getValue
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import durdinapps.rxfirebase2.RxFirebaseDatabase
import durdinapps.rxfirebase2.RxFirebaseFunctions
import durdinapps.rxfirebase2.RxFirebaseStorage
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import java.io.File
import java.util.*

/**
 * Created by Devlomi on 01/08/2017.
 */

class FireManager {
    //check if this user has installed this app and return user object if user is exists

    fun fetchAndSaveUserByPhone(phone: String): Maybe<User?> {
        //check if the number contains denied characters
        if (isHasDeniedFirebaseStrings(phone)) return Maybe.error(Throwable("HasDeniedFirebaseStrings"))
        //get phone number and start searching for this phone number
        val query = FireConstants.uidByPhone.child(phone)
        return query.observeSingleValueEvent().flatMap { dataSnapshot ->

            if (dataSnapshot.value == null || dataSnapshot.value is Map<*, *>) {
                return@flatMap Maybe.empty<User>()
            }

            val uid = dataSnapshot.getValue(String::class.java) ?: return@flatMap Maybe.just(null)
            FireConstants.usersRef.child(uid).observeSingleValueEvent().map { userSnapshot ->

                val user = userSnapshot.getValue(User::class.java)
                //set user uid
                user?.uid = dataSnapshot.value as? String
                val context = MyApp.context()
                user?.userName = ContactUtils.queryForNameByNumber(phone)
                user?.isStoredInContacts = ContactUtils.contactExists(context, user?.phone)

                user

            }.doOnSuccess {
                it?.let { user ->
                    RealmHelper.getInstance().saveObjectToRealm(user)
                }
            }


        }


    }


    //set the current presence as Online

    fun setOnlineStatus(): Completable {
        return RxFirebaseDatabase.setValue(FireConstants.presenceRef.child(uid), "Online").doOnComplete {
            SharedPreferencesManager.setLastSeenState(LastSeenStates.ONLINE)
        }
    }


    fun downloadCurrentUserPhoto(photoUrl: String): Single<String> {

        if (photoUrl == "") {
            return Single.error(Throwable("already downloading"))
        }


        val ref = FirebaseStorage.getInstance().getReferenceFromUrl(photoUrl)


        val filePath = DirManager.generateUserProfileImage()



        return ref.getFileRx(filePath).map { filePath.path }


    }

    //update user text status

    fun updateMyStatus(status: String): Completable {
        return FireConstants.usersRef.child(uid).child("status").setValueRx(status)
    }

    fun fetchUserStatus(uid: String): Maybe<String?> {
        return FireConstants.usersRef.child(uid).child("status").observeSingleValueEvent().map {
            it.value as? String
        }.doOnSuccess {
            if (it != null)
                RealmHelper.getInstance().updateUserStatus(uid, it)
        }
    }

    //update user username

    fun updateMyUserName(username: String): Completable {
        return RxFirebaseDatabase.setValue(FireConstants.usersRef.child(uid).child("name"), username)
    }

    //this will upload the user photo that he picked and generate a Small circle image and decode as base64

    fun updateMyPhoto(imagePath: String): Maybe<Triple<String, String, String>> {
        //generate new name for the file when uploading to firebase storage
        val fileName = UUID.randomUUID().toString() + Util.getFileExtensionFromPath(imagePath)
        //upload image
        val ref = FireConstants.imageProfileRef.child(fileName)

        val file = Uri.fromFile(File(imagePath))

        return RxFirebaseStorage.putFile(ref, file).flatMapMaybe {
            return@flatMapMaybe RxFirebaseStorage.getDownloadUrl(ref)

        }.flatMap {
            val downloadUrl = it.toString()
            val updateMap: MutableMap<String, Any> = HashMap()

            //generate circle bitmap
            val circleBitmap = BitmapUtils.getCircleBitmap(BitmapUtils.convertFileImageToBitmap(imagePath))
            //decode the image as base64 string
            val decodedImage = BitmapUtils.decodeImageAsPng(circleBitmap)
            SharedPreferencesManager.saveMyThumbImg(decodedImage)
            SharedPreferencesManager.saveMyPhoto(imagePath)

            //add the photo to the map
            updateMap["photo"] = downloadUrl
            //add the thumb circle image to the map
            updateMap["thumbImg"] = decodedImage

            //save them in firebase database using one request
            return@flatMap RxFirebaseDatabase.updateChildren(FireConstants.usersRef.child(uid), updateMap).andThen(
                    Maybe.just(Triple(decodedImage, file.path!!, downloadUrl))
            )
        }

    }


    fun setUserBlocked(uid: String, receiverUid: String, setBlocked: Boolean): Completable {
        val ref = FireConstants.blockedUsersRef.child(uid).child(receiverUid)

        val value = if (setBlocked) true else null
        return ref.setValueRx(value)

    }

    //set all unread messages in a Chat as Read

    fun setMessagesAsRead(context: Context, chatId: String) {
        //get unread messages
        val results = RealmHelper.getInstance().getUnReadIncomingMessages(chatId)
        for (message in results) {
            ServiceHelper.startUpdateMessageStatRequest(context, message.messageId, uid, chatId, MessageStat.READ)
        }
    }

    //set last seen value,this will set value at the Server Time
    //so if the device clock is not correct it will not affect the last seen value

    fun setLastSeen(): Completable {

        return RxFirebaseDatabase.setValue(FireConstants.presenceRef.child(uid), ServerValue.TIMESTAMP).doOnComplete {
            SharedPreferencesManager.setLastSeenState(LastSeenStates.LAST_SEEN)
        }
    }

    //set the typing or recording or do nothing state

    fun setTypingStat(receiverUid: String, stat: Int, isGroup: Boolean, isBroadcast: Boolean): Completable {

        if (isBroadcast) return Completable.complete()

        return if (isGroup) {
            RxFirebaseDatabase.setValue(FireConstants.groupTypingStat.child(receiverUid).child(uid), stat)
        } else {
            RxFirebaseDatabase.setValue(FireConstants.typingStat.child(receiverUid), stat)
        }
    }


    //update message state as received or read

    private fun updateMessageStat(myUid: String, messageId: String, stat: Int): Completable {
        return RxFirebaseDatabase.setValue(FireConstants.messageStat.child(myUid)
                .child(messageId), stat)
                .doOnComplete {
                    RealmHelper.getInstance().updateMessageStatLocally(messageId, stat)
                    RealmHelper.getInstance().deleteUnUpdateStat(messageId)
                }

    }

    fun updateMessagesState(myUid: String, messageId: String, state: Int, isVoiceMessage: Boolean): Completable {
        return updateMessageStat(myUid, messageId, state)
                .andThen(Observable.fromIterable(RealmHelper.getInstance().unUpdateMessageStat))
                .flatMapCompletable { unUpdatedStat ->
                    return@flatMapCompletable updateMessageStat(unUpdatedStat.myUid, unUpdatedStat.messageId, unUpdatedStat.statToBeUpdated).andThen {
                        RealmHelper.getInstance().updateMessageStatLocally(unUpdatedStat.messageId, unUpdatedStat.statToBeUpdated)
                        RealmHelper.getInstance().deleteUnUpdateStat(unUpdatedStat.messageId)
                        RealmHelper.getInstance().deleteJobId(unUpdatedStat.messageId, isVoiceMessage)

                    }
                }
    }
//update voice message state as Seen

    fun updateVoiceMessageStat(myUid: String, messageId: String): Completable {
        val ref = FireConstants.voiceMessageStat.child(myUid).child(messageId)
        return RxFirebaseDatabase.setValue(ref, true)
                .doOnComplete {
                    RealmHelper.getInstance().updateVoiceMessageStatLocally(messageId)
                    RealmHelper.getInstance().deleteUnUpdatedVoiceMessageStat(messageId)

                }.doOnError {
                    RealmHelper.getInstance().saveUnUpdatedVoiceMessageStat(myUid, messageId, true)
                }
    }


    fun isCallCancelled(userId: String, callId: String): Single<Boolean> {
        return FireConstants.callsRef.child(uid).child(userId).child(callId).snapshotAtRefExists()
    }


    fun setCallCancelled(userId: String, callId: String): Completable {
        return FireConstants.callsRef.child(uid).child(userId).child(callId).setValueRx(true)
    }


    fun getServerTime(): Single<Long> {
        return RxFirebaseFunctions.getHttpsCallable(FirebaseFunctions.getInstance(), "getTime").map { task ->
            return@map task.data as? Long
        }

    }


    fun isUserBlocked(otherUserUid: String): Single<Boolean> {
        return FireConstants.blockedUsersRef.child(otherUserUid).child(uid).snapshotAtRefExists()
    }

    fun downloadUserPhoto(uid: String, oldLocalPath: String?, isGroup: Boolean): Single<String> {
        if (imageDownloadProcessIds.contains(uid)) return Single.error(Throwable("already downloading"))

        imageDownloadProcessIds.add(uid)
        val ref = if (isGroup) FireConstants.groupsRef.child(uid).child("info") else FireConstants.usersRef.child(uid)

        val imagePath = DirManager.generateUserProfileImage()

        var foundPhoto: String? = null
        return RxFirebaseDatabase.observeSingleValueEvent(ref.child("photo")).toSingle().flatMap { dataSnapshot ->
            if (dataSnapshot.exists()) {
                val photo = dataSnapshot.getValue(String::class.java)
                val referenceFromUrl = FirebaseStorage.getInstance().getReferenceFromUrl(photo!!)
                foundPhoto = photo
                return@flatMap RxFirebaseStorage.getFile(referenceFromUrl, imagePath)

            } else {
                return@flatMap Single.error<String>(Throwable("Not exists"))
            }

        }.map {
            imagePath.path
        }.doOnSuccess {
            foundPhoto?.let { photo ->
                RealmHelper.getInstance().updateUserImg(uid, photo, imagePath.path, oldLocalPath)

            }

        }.doFinally {
            imageDownloadProcessIds.remove(uid)
        }


    }

    companion object {

        //fix for com.google.firebase.database.DatabaseException: Invalid Firebase Database path: #21#.
        // Firebase Database paths must not contain '.', '#', '$', '[', or ']'
        //if a phone number contains one of these characters we will skip this number since it's not a Phone Number
        private val deniedFirebaseStrings = arrayOf(".", "#", "$", "[", "]")

        const val STATUS_TYPE = 8888

        //every user image download request will saved here to prevent download the same image over and over
        private val imageDownloadProcessIds: MutableList<String> = ArrayList()

        //is this user is logged in
        @JvmStatic
        fun isLoggedIn(): Boolean {
            return FirebaseAuth.getInstance().currentUser != null
        }
        //get this user's uid

        @JvmStatic
        val uid: String
            get() = FirebaseAuth.getInstance().currentUser!!.uid

        @JvmStatic
        fun isAdmin(adminUids: List<String?>): Boolean {
            return adminUids.contains(uid)
        }

        @JvmStatic
        fun isAdmin(adminUid: String?, adminUids: List<String?>): Boolean {
            return adminUids.contains(adminUid)
        }

        //get this user's phone number

        @JvmStatic
        val phoneNumber: String
            get() = FirebaseAuth.getInstance().currentUser!!.phoneNumber!!

        //will check if phone number has one of these strings
        @JvmStatic
        fun isHasDeniedFirebaseStrings(deniedString: String): Boolean {
            for (deniedFirebaseString in deniedFirebaseStrings) {
                if (deniedString.contains(deniedFirebaseString)) {
                    return true
                }
            }
            return false
        }

        //get correct ref for the given type

        @JvmStatic
        fun getRef(type: Int, fileName: String?): StorageReference {
            val mName = UUID.randomUUID().toString() + "." + Util.getFileExtensionFromPath(fileName)
            when (type) {
                MessageType.SENT_IMAGE -> return FireConstants.imageRef.child(mName)
                MessageType.SENT_VIDEO -> return FireConstants.videoRef.child(mName)
                MessageType.SENT_VOICE_MESSAGE -> return FireConstants.voiceRef.child(mName)
                MessageType.SENT_AUDIO -> return FireConstants.audioRef.child(mName)
                STATUS_TYPE -> return FireConstants.statusStorageRef.child(mName)
            }
            return FireConstants.fileRef.child(mName)
        }

        @JvmStatic
        fun fetchUserByUid(uid: String): Maybe<User?> {

            val query = FireConstants.usersRef.child(uid)

            return RxFirebaseDatabase.observeSingleValueEvent(query).map { dataSnapshot ->
                val user = dataSnapshot.getValue<User>()

                val context = MyApp.context()
                //set user uid
                user?.let {
                    val phone = it.phone ?: ""
                    it.uid = dataSnapshot.ref.key
                    it.userName = ContactUtils.queryForNameByNumber(phone)
                    it.isStoredInContacts = ContactUtils.contactExists(context, phone)

                    return@map user
                }
            }.doOnSuccess {
                it?.let { user ->
                    RealmHelper.getInstance().saveObjectToRealm(user)
                }

            }


        }

        //check if there is a new photo for this user and download it
        //check for both thumb and full photo
        @JvmStatic
        fun checkAndDownloadUserPhoto(user: User?): Observable<ImageItem> {
            if (user == null) return Observable.error(Throwable("User is null"))


            val databaseReference = if (user.isGroupBool) FireConstants.groupsRef.child(user.uid).child("info") else FireConstants.usersRef.child(user.uid)

            return Observable.create { emitter ->
                databaseReference.observeSingleValueEvent().subscribe({ dataSnapshot: DataSnapshot ->
                    if (!dataSnapshot.exists()) {
                        emitter.onError(Throwable("Snapshot Not Exists"))
                        return@subscribe
                    }
                    val photo = dataSnapshot.child("photo").getValue(String::class.java)
                    val thumbImg = dataSnapshot.child("thumbImg").getValue(String::class.java)

                    if (user.thumbImg == null) {
                        RealmHelper.getInstance().updateThumbImg(user.uid, thumbImg)
                        emitter.onNext(ImageItem(thumbImg, null))
                    } else if (user.thumbImg != null && user.thumbImg != thumbImg) {
                        RealmHelper.getInstance().updateThumbImg(user.uid, thumbImg)
                        emitter.onNext(ImageItem(thumbImg, null))
                    }
                    if (user.photo != null && photo != user.photo || !FileUtils.isFileExists(user.userLocalPhoto)) {
                        downloadUserPhoto(photo, user.uid, user.userLocalPhoto).subscribe({ photoPath ->
                            emitter.onNext(ImageItem(null, photoPath))
                            emitter.onComplete()
                        }, { throwable ->

                        })

                    } else {
                        emitter.onComplete()
                    }

                }, { throwable ->
                    emitter.onError(throwable)
                })
            }


        }


        //check only for thumb img
        @JvmStatic
        fun checkAndDownloadUserThumbImg(user: User?): Maybe<String?> {
            if (user == null) return Maybe.error(Throwable("user is null"))

            val databaseReference = if (user.isGroupBool) FireConstants.groupsRef.child(user.uid).child("info") else FireConstants.usersRef.child(user.uid)
            return RxFirebaseDatabase.observeSingleValueEvent(databaseReference.child("thumbImg")).map { dataSnapshot ->
                val thumbImg = dataSnapshot.getValue(String::class.java)
                if (user.thumbImg == null) {
                    RealmHelper.getInstance().updateThumbImg(user.uid, thumbImg)
                } else
                    if (user.thumbImg != null && user.thumbImg != thumbImg) {
                        RealmHelper.getInstance().updateThumbImg(user.uid, thumbImg)

                    }
                return@map thumbImg
            }.doOnSuccess {
                RealmHelper.getInstance().setLastImageSyncDate(user.uid, Date().time)
            }
        }

        @JvmStatic
        fun downloadUserPhoto(photo: String?, uid: String, oldLocalPath: String?): Single<String> {
            if (photo == null || imageDownloadProcessIds.contains(uid)) return Single.error(Throwable("Already Downloading"))
            val referenceFromUrl = FirebaseStorage.getInstance().getReferenceFromUrl(photo)
            val imagePath = DirManager.generateUserProfileImage()
            imageDownloadProcessIds.add(uid)

            return RxFirebaseStorage.getFile(referenceFromUrl, imagePath).map { imagePath.path }.doFinally {
                imageDownloadProcessIds.remove(uid)
            }.doOnSuccess {
                //save user image to realm if it's not the same
                RealmHelper.getInstance().updateUserImg(uid, photo, imagePath.path, oldLocalPath)

            }

        }

        @JvmStatic
        fun generateKey() = FireConstants.mainRef.push().key!!


    }
}


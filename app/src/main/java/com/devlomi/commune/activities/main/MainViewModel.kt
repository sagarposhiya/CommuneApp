package com.devlomi.commune.activities.main

import android.app.Application
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.devlomi.commune.activities.main.status.StatusFragmentEvent
import com.devlomi.commune.common.DisposableAndroidViewModel
import com.devlomi.commune.common.extensions.toDeffered
import com.devlomi.commune.extensions.setValueRx
import com.devlomi.commune.extensions.snapshotAtRefExists
import com.devlomi.commune.job.DeleteStatusJob
import com.devlomi.commune.model.realms.TextStatus
import com.devlomi.commune.model.constants.StatusType
import com.devlomi.commune.model.realms.Status
import com.devlomi.commune.model.realms.User
import com.devlomi.commune.utils.FireConstants
import com.devlomi.commune.utils.RealmHelper
import com.devlomi.commune.utils.SharedPreferencesManager
import com.devlomi.commune.utils.TimeHelper
import com.devlomi.commune.utils.network.FireManager
import com.devlomi.commune.utils.update.UpdateChecker
import com.google.firebase.database.DataSnapshot
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class MainViewModel(private val context: Application) : DisposableAndroidViewModel(context) {

    private val realmHelper = RealmHelper.getInstance()
    var lastSyncTime = 0L

    private val _statusLiveData = MutableLiveData<StatusFragmentEvent>()
    val statusLiveData: LiveData<StatusFragmentEvent>
        get() = _statusLiveData

    private val _queryTextChange = MutableLiveData<String>()
    val queryTextChange: LiveData<String>
        get() = _queryTextChange

    fun onQueryTextChange(text: String) {
        _queryTextChange.value = text
    }


    companion object {
        //15Sec
        const val WAIT_TIME = 15000
    }

    fun fetchStatuses(users: List<User>) {

        //wait for 15 sec before re-fetching statuses
        if (lastSyncTime == 0L || System.currentTimeMillis() - lastSyncTime > WAIT_TIME) {
            viewModelScope.launch {
                try {
                    val statusesIds = mutableListOf<String>()
                    fetchImageAndVideosStatuses(users, statusesIds)
                    fetchTextStatuses(users, statusesIds)
                    realmHelper.deleteDeletedStatusesLocally(statusesIds)
                    lastSyncTime = System.currentTimeMillis()
                    updateUi()

                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }
    }

    private fun updateUi() {
        _statusLiveData.value = StatusFragmentEvent.StatusInsertedEvent()
    }


    private fun handleStatus(dataSnapshot: DataSnapshot, statusesIds: MutableList<String>) {


        if (dataSnapshot.value != null) {
            //get every status
            for (snapshot in dataSnapshot.children) {
                val userId = snapshot.ref.parent!!.key
                val statusId = snapshot.key
                val status = snapshot.getValue(Status::class.java)
                status!!.statusId = statusId
                status.userId = userId

                if (status.type == StatusType.TEXT) {
                    val textStatus = snapshot.getValue(TextStatus::class.java)
                    textStatus!!.statusId = statusId!!
                    status.textStatus = textStatus
                }

                statusesIds.add(statusId!!)
                //check if status is exists in local database , if not save it
                if (realmHelper.getStatus(status.statusId) == null) {
                    realmHelper.saveStatus(userId, status)
                    //schedule a job after 24 hours to delete this status locally
                    DeleteStatusJob.schedule(userId, statusId)
                }


            }

        }
    }

    private suspend fun fetchImageAndVideosStatuses(users: List<User>, statusesIds: MutableList<String>) {
        //add all statuses to this list to delete deleted statuses if needed
        //get current time before 24 hours (Yesterday)
        val timeBefore24Hours = TimeHelper.getTimeBefore24Hours()
        //get all user statuses that are not passed 24 hours


        val jobs = mutableListOf<Deferred<DataSnapshot>>()


        val job = viewModelScope.async {
            for (user in users!!) {
                val query = FireConstants.statusRef.child(user.uid)
                        .orderByChild("timestamp")
                        .startAt(timeBefore24Hours.toDouble())


                val dataSnapshot = query.toDeffered()

                jobs.add(dataSnapshot)
            }

        }


        job.await()
        val datasnapshots = jobs.awaitAll()
        datasnapshots.forEach {
            handleStatus(it, statusesIds)
        }


    }

    private suspend fun fetchTextStatuses(users: List<User>, statusesIds: MutableList<String>) {
//        val statusesIds = mutableListOf<String>()

        //add all statuses to this list to delete deleted statuses if needed
        //get current time before 24 hours (Yesterday)
        val timeBefore24Hours = TimeHelper.getTimeBefore24Hours()
        //get all user statuses that are not passed 24 hours


        val jobs = mutableListOf<Deferred<DataSnapshot>>()

        val job = viewModelScope.async {
            for (user in users!!) {
                val query = FireConstants.textStatusRef.child(user.uid)
                        .orderByChild("timestamp")
                        .startAt(timeBefore24Hours.toDouble())


                val dataSnapshot = query.toDeffered()

                jobs.add(dataSnapshot)
            }

        }


        job.await()
        val datasnapshots = jobs.awaitAll()
        datasnapshots.forEach {
            handleStatus(it, statusesIds)
        }


    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        data?.let {
            _statusLiveData.value = StatusFragmentEvent.OnActivityResultEvent(requestCode, resultCode, data)
        }

    }

    //if user is coming from an old version, then delete the already received messages from his db
    fun deleteOldMessagesIfNeeded() {

        if (!SharedPreferencesManager.isDeletedUnfetchedMessage()) {

            val deleteUserMessages = FireConstants.userMessages.child(FireManager.uid).setValueRx(null)
            val deleteDeletedMessages = FireConstants.deletedMessages.child(FireManager.uid).setValueRx(null)
            val deleteNewGroupsEvents = FireConstants.newGroups.child(FireManager.uid).setValueRx(null)
            val setDeletedOldMessagesToTrue = FireConstants.hasDeletedOldMessages.child(FireManager.uid).setValueRx(true)
            val completable = Completable.merge(arrayListOf(deleteUserMessages, deleteDeletedMessages, deleteNewGroupsEvents))

            FireConstants.hasDeletedOldMessages.child(FireManager.uid).snapshotAtRefExists().flatMapCompletable { isExists ->

                if (isExists) {
                    return@flatMapCompletable Completable.complete()
                }

                return@flatMapCompletable completable


            }.andThen(setDeletedOldMessagesToTrue)
                    .doOnComplete {
                        SharedPreferencesManager.setDeletedUnfetchedMessage(true)
                    }.subscribe({

                    }, { throwable ->

                    }).addTo(disposables)
        }
    }

    fun checkForUpdate(): Maybe<Boolean> {
        return UpdateChecker(context).checkForUpdate()
    }


}



package com.devlomi.commune.activities.main.messaging

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.devlomi.commune.model.AudibleState
import com.devlomi.commune.model.constants.MessageType
import com.devlomi.commune.model.realms.Message
import com.devlomi.commune.utils.Util

class ChatViewModel : ViewModel() {


    private val _itemSelectedLiveData = MutableLiveData<List<Message>>()
    val itemSelectedLiveData: LiveData<List<Message>> = _itemSelectedLiveData

    private val _progressMapLiveData = MutableLiveData<Map<String, Int>>()
    val progressMapLiveData: LiveData<Map<String, Int>> = _progressMapLiveData


    private val _audibleState = MutableLiveData<Map<String, AudibleState>>()
    val audibleState: LiveData<Map<String, AudibleState>> = _audibleState

    private val _selectedItems = arrayListOf<Message>()
    val selectedItems: List<Message> = _selectedItems
    private val progressMap = mutableMapOf<String, Int>()
    private val audibleMap = mutableMapOf<String, AudibleState>()

    fun itemSelected(pos: Int, message: Message) {
        if (_selectedItems.contains(message))
            _selectedItems.remove(message)
        else {
            _selectedItems.add(message)
        }


        _itemSelectedLiveData.value = _selectedItems

    }

    fun networkProgressChanged(messageId: String, progress: Int) {
        progressMap[messageId] = progress
        _progressMapLiveData.value = progressMap
    }

    fun removeNetworkProgress(messageId: String) {
        progressMap.remove(messageId)
        _progressMapLiveData.value = progressMap
    }

    fun setAudibleMax(messageId: String, max: Int) {

        val recyclerStateOrNew = getRecyclerStateOrNew(messageId)
        recyclerStateOrNew.max = max
        audibleMap[messageId] = recyclerStateOrNew
        _audibleState.value = audibleMap
    }

    fun setAudiblePlayState(messageId: String, isPlaying: Boolean) {
        val recyclerStateOrNew = getRecyclerStateOrNew(messageId)

        recyclerStateOrNew.isPlaying = isPlaying

        recyclerStateOrNew.progress = getAudibleProgressForId(messageId)

        audibleMap[messageId] = recyclerStateOrNew
        _audibleState.value = audibleMap
    }

    fun setAudibleComplete(messageId: String, finalProgress: Int) {
        val recyclerStateOrNew = getRecyclerStateOrNew(messageId)

        recyclerStateOrNew.isPlaying = false
        recyclerStateOrNew.progress = finalProgress
        val currentDuration = Util.milliSecondsToTimer(finalProgress.toLong())

        recyclerStateOrNew.currentDuration = currentDuration
        audibleMap[messageId] = recyclerStateOrNew
        _audibleState.value = audibleMap
    }


    fun setAudibleProgress(messageId: String, progress: Int, waves: ByteArray? = null) {
        val recyclerStateOrNew = getRecyclerStateOrNew(messageId)
        recyclerStateOrNew.progress = progress
        val currentDuration = Util.milliSecondsToTimer(progress.toLong())

        if (waves != null)
            recyclerStateOrNew.waves = waves
        recyclerStateOrNew.currentDuration = currentDuration


        audibleMap[messageId] = recyclerStateOrNew
        _audibleState.value = audibleMap
    }

    private fun getRecyclerStateOrNew(messageId: String): AudibleState {
        return audibleMap[messageId] ?: AudibleState()
    }

    fun getAudibleProgressForId(messageId: String): Int {
        return audibleMap[messageId]?.progress ?: -1
    }

    fun clearSelectedItems() {
        _selectedItems.clear()
    }

    fun isSelectedItemsContainMedia(): Boolean {
        return selectedItems.filter { MessageType.isMediaItem(it.type) }.isNotEmpty()
    }
}
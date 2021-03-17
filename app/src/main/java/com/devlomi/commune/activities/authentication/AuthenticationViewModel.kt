package com.devlomi.commune.activities.authentication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AuthenticationViewModel : ViewModel() {
    private val _liveData = MutableLiveData<ViewState>()
    val liveData: LiveData<ViewState>
        get() = _liveData


    fun handleStateEvent(stateEvent: StateEvent) {
        when (stateEvent) {
            is StateEvent.NumberEntered -> {
                _liveData.value = ViewState.Verify(stateEvent.phoneNumber)
            }
        }
    }
}
package com.devlomi.commune.activities.authentication

sealed class ViewState {
    data class Verify(val phoneNumber: String) : ViewState()


}
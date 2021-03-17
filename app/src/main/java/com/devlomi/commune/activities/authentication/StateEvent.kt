package com.devlomi.commune.activities.authentication

import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential

sealed class StateEvent {
    data class NumberEntered(val phoneNumber:String):StateEvent()

    sealed class AuthResult : StateEvent() {
        data class VerificationCompleted(val authCredential: PhoneAuthCredential) : AuthResult()
        data class VerificationFailed(val exception: FirebaseException) : AuthResult()
        data class CodeSent(val code:String) : AuthResult()
        data class CodeAutoRetrievalTimeOut(val p0:String) : AuthResult()
    }
}
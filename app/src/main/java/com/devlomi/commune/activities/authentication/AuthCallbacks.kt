package com.devlomi.commune.activities.authentication

interface AuthCallbacks {
    fun verifyPhoneNumber(phoneNumber: String,countryIso:String)
    fun verifyCode(code:String)
    fun cancelVerificationRequest()
}
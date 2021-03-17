package com.devlomi.commune.utils.network

import android.app.Activity
import com.devlomi.commune.utils.MyApp
import com.google.firebase.auth.PhoneAuthProvider
import io.michaelrocks.libphonenumber.android.NumberParseException
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import io.michaelrocks.libphonenumber.android.Phonenumber
import java.util.concurrent.TimeUnit

class AuthManager {
    fun verify(phoneNumber: String, activity: Activity, callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(phoneNumber,
                60, // Timeout duration
                TimeUnit.SECONDS, // Unit of timeout
                activity, // Activity (for callback binding)
                callbacks
        )


    }




    fun formatNumber(number:String,countryCode:String): String? {
            val context = MyApp.context()
            val util = PhoneNumberUtil.createInstance(context)

            val phoneNumber: Phonenumber.PhoneNumber
            var phone: String?=null
            try {
                //format number depending on user's country code
                phoneNumber = util.parse(number, countryCode)
                phone = util.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164)
            } catch (e: NumberParseException) {
                e.printStackTrace()
            }


            return phone

    }
}
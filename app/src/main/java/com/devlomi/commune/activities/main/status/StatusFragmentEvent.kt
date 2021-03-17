package com.devlomi.commune.activities.main.status

import android.content.Intent

sealed class StatusFragmentEvent {

     class OnActivityResultEvent(val requestCode:Int, val resultCode:Int, val data:Intent):StatusFragmentEvent()
    class StatusInsertedEvent():StatusFragmentEvent()
}
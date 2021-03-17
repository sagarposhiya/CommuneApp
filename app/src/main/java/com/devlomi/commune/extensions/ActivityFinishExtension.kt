/*
 * Created by Devlomi on 2020
 */

package com.devlomi.commune.extensions

import android.app.Activity
import android.content.Intent
import android.os.Bundle

fun Activity.finishAndRemoveTaskCompat() {
    if (android.os.Build.VERSION.SDK_INT >= 21) {
        finishAndRemoveTask()
    } else {
        val intent = Intent(this, ExitAndRemoveFromRecentAppsDummyActivity::class.java)

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                Intent.FLAG_ACTIVITY_NO_ANIMATION or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)

        startActivity(intent)
    }
}

class ExitAndRemoveFromRecentAppsDummyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}


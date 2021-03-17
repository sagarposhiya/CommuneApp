package com.devlomi.commune.utils.update

import android.content.Context
import androidx.core.content.edit
import com.devlomi.commune.BuildConfig
import com.devlomi.commune.extensions.observeSingleValueEvent
import com.devlomi.commune.utils.FireConstants
import com.google.firebase.database.ktx.getValue
import io.reactivex.Maybe

class UpdateChecker(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("update", Context.MODE_PRIVATE)

    companion object {
        private var LOCK = false
    }

    fun checkForUpdate(): Maybe<Boolean> {
        if (LOCK) {
            return Maybe.empty()
        } else {
            LOCK = true
        }

        return FireConstants.updateRef.observeSingleValueEvent().map {
            val value = it.getValue<UpdateInfo>()
            return@map value
        }.filter { it != null }.map { updateInfo ->
            val currentAppVersion = BuildConfig.VERSION_CODE

            val latestVersion = updateInfo.latestVersion
            val versionsToUpdate = updateInfo.versionsToUpdate

            if (latestVersion == currentAppVersion) {
                return@map false
            }

            when (updateInfo.updateCondition) {
                UpdateConditions.ONLY -> {
                    if (versionsToUpdate == currentAppVersion) {
                        return@map true
                    }
                }

                UpdateConditions.AND_ABOVE -> {
                    if (currentAppVersion >= versionsToUpdate) {
                        return@map true
                    }
                }

                UpdateConditions.AND_BELOW -> {
                    if (currentAppVersion <= versionsToUpdate) {
                        return@map true
                    }
                }

                UpdateConditions.NONE -> {
                    return@map false
                }
            }

            return@map false

        }.doOnSuccess { shouldUpdate ->
            saveUpdateMode(shouldUpdate)
        }.doFinally {
            LOCK = false
        }

    }

    private fun saveUpdateMode(shouldUpdate: Boolean) {
        sharedPreferences.edit {
            putBoolean("shouldUpdate", shouldUpdate)
        }
    }

    fun needsUpdate(): Boolean {
        return sharedPreferences.getBoolean("shouldUpdate", false)
    }

}
package com.devlomi.commune.activities.settings

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.devlomi.commune.R
import com.devlomi.commune.utils.*
import com.devlomi.commune.views.dialogs.IgnoreBatteryDialog


/**
 * This fragment shows notify preferences only. It is used when the
 * activity is showing a two-pane settings UI.
 */
class NotificationPreferenceFragment : PreferenceFragmentCompat() {

    companion object {
        const val KEY_RINGTONE_PREFERENCE = "notifications_new_message_ringtone"
        const val REQUEST_CODE_ALERT_RINGTONE = 485

    }

    private var ringtonePreference: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {


        addPreferencesFromResource(R.xml.pref_notification)
        setHasOptionsMenu(true)

        // Bind the summaries of EditText/List/Dialog/Ringtone preferences
        // to their values. When their values change, their summaries are
        // updated to reflect the new value, per the Android Design
        // guidelines.
        ringtonePreference = findPreference(KEY_RINGTONE_PREFERENCE)
        SettingsActivity.bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"))
        findPreference<Preference>("ignore_battery")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val ignoreBatteryDialog = IgnoreBatteryDialog(activity)
            ignoreBatteryDialog.setOnDialogClickListener(object : IgnoreBatteryDialog.OnDialogClickListener {
                override fun onCancelClick(checkBoxChecked: Boolean) {
                    SharedPreferencesManager.setDoNotShowBatteryOptimizationAgain(checkBoxChecked)
                }

                override fun onOk() {
                    try {
                        val intent = Intent()
                        intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                        startActivity(intent)
                    } catch (e: Exception) {

                    }
                }
            })
            ignoreBatteryDialog.show()
            false
        }

        if (Util.isOreoOrAbove()) {
            ringtonePreference?.summary = null
        }


    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return if (id == android.R.id.home) {
            true
        } else super.onOptionsItemSelected(item)
    }


    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        return if (preference.key == KEY_RINGTONE_PREFERENCE) {
            if (Build.VERSION.SDK_INT < 26) {
                startRingtonePicker()
            } else {
                startNotificationChannelSettings()
            }
            true
        } else {
            super.onPreferenceTreeClick(preference)
        }
    }

    private fun startNotificationChannelSettings() {
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, NotificationHelper.NOTIFICATION_CHANNEL_ID_MESSAGES)
        }
        startActivity(intent)
    }

    private fun startRingtonePicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI)
        val existingValue: String = SharedPreferencesManager.getRingtone().toString()
        if (existingValue != null) {
            if (existingValue.isEmpty()) {
                // Select "Silent"
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, null as Uri?)
            } else {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingValue))
            }
        } else {
            // No ringtone has been selected, set to the default
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Settings.System.DEFAULT_NOTIFICATION_URI)
        }
        startActivityForResult(intent, REQUEST_CODE_ALERT_RINGTONE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_ALERT_RINGTONE && data != null) {
            val ringtone: Uri? = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (ringtone != null) {

                val audioPath = RealPathUtil.getAudioPath(activity, ringtone)[0]
                if (audioPath != null) {
                    ringtonePreference?.summary = ringtone.toString()
                    SharedPreferencesManager.setRingtone(ringtone.toString())
                }
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }


}


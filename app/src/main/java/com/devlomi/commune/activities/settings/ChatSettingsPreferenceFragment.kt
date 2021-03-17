package com.devlomi.commune.activities.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.devlomi.commune.R
import com.devlomi.commune.activities.BackupChatActivity
import com.devlomi.commune.utils.DirManager
import com.devlomi.commune.utils.FileUtils
import com.devlomi.commune.utils.SharedPreferencesManager
import com.theartofdev.edmodo.cropper.CropImage
import java.io.IOException

class ChatSettingsPreferenceFragment : PreferenceFragmentCompat() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {


        addPreferencesFromResource(R.xml.pref_chat)
        findPreference<Preference>("wallpaper_path")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val dialog = AlertDialog.Builder(requireActivity())
            dialog.setPositiveButton(R.string.choose_wallpaper) { dialogInterface, i ->
                CropImage.activity()
                        .start(requireActivity(), this@ChatSettingsPreferenceFragment)
            }.setNegativeButton(R.string.restore_default_wallpaper) { dialogInterface, i -> SharedPreferencesManager.setWallpaperPath("") }.show()
            false
        }
        findPreference<Preference>("chat_backup")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            startActivity(Intent(activity, BackupChatActivity::class.java))
            false
        }
        setHasOptionsMenu(true)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == Activity.RESULT_OK) {
                val resultUri = result.uri
                val file = DirManager.genereateWallpaperFile()
                try {
                    //copy image to the Wallpaper Folder
                    FileUtils.copyFile(resultUri.path, file)
                    SharedPreferencesManager.setWallpaperPath(file.path)
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(activity, R.string.error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return if (id == android.R.id.home) {
            true
        } else super.onOptionsItemSelected(item)
    }
} //

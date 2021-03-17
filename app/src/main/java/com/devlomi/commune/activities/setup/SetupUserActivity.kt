package com.devlomi.commune.activities.setup

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.devlomi.commune.BuildConfig
import com.devlomi.commune.R
import com.devlomi.commune.activities.main.MainActivity
import com.devlomi.commune.exceptions.BackupFileMismatchedException
import com.devlomi.commune.exceptions.NoDefaultImageException
import com.devlomi.commune.utils.*
import com.google.android.material.snackbar.Snackbar
import com.theartofdev.edmodo.cropper.CropImage
import io.realm.exceptions.RealmMigrationNeededException
import kotlinx.android.synthetic.main.activity_setup_user.*
import java.io.IOException

class SetupUserActivity : AppCompatActivity() {


    private var choosenPhoto: String? = null
    private var progressDialog: ProgressDialog? = null

    private val viewModel: SetupUserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_user)


        viewModel.completeSetupLiveData.observe(this, androidx.lifecycle.Observer { pair ->
            progressDialog?.dismiss()
            val isSuccess = pair.first
            val throwable = pair.second
            if (isSuccess) {
                startMainActivity()
            } else {
                if (throwable != null) {
                    if (throwable is NoDefaultImageException && BuildConfig.DEBUG) {
                        Toast.makeText(this, "Please upload Default User Image", Toast.LENGTH_SHORT).show()
                    } else {
                        showSnackbar()
                    }
                }
            }
        })


        viewModel.loadUserImageLiveData.observe(this, androidx.lifecycle.Observer { photoUrl ->
            loadUserPhoto(photoUrl)
        })

        fab_setup_user.setOnClickListener {
            KeyboardHelper.hideSoftKeyboard(this, et_username_setup)
            completeSetup()
        }
        user_img_setup.setOnClickListener { pickImage() }

        //On Done Keyboard Button Click
        et_username_setup.setOnEditorActionListener(TextView.OnEditorActionListener
        { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                completeSetup()
                return@OnEditorActionListener true
            }
            false
        })


        if (RealmBackupRestore.isBackupFileExists()) {
            check_text_view_number.visibility = View.VISIBLE
        } else {
            check_text_view_number.visibility = View.GONE
        }

        viewModel.fetchUserImage()

    }

    private fun loadUserPhoto(photoUrl: String) {
        //load the image
        //we are using listener to determine when the image loading is finished
        //so we can hide the progressBar

        Glide.with(this@SetupUserActivity).load(photoUrl)
                .listener(object : RequestListener<Drawable> {
                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        progress_bar_setup_user_img.visibility = View.GONE
                        return false
                    }

                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        progress_bar_setup_user_img.visibility = View.GONE
                        return false;
                    }

                }).into(user_img_setup)
    }


    private fun completeSetup() {
        //check if user not entered his username
        if (TextUtils.isEmpty(et_username_setup.text.toString())) {
            et_username_setup.error = getString(R.string.username_is_empty)
        } else {

            progressDialog = ProgressDialog(this@SetupUserActivity)
            progressDialog?.setMessage(getString(R.string.loading))
            progressDialog?.setCancelable(false)
            progressDialog?.show()

            if (check_text_view_number.visibility == View.VISIBLE && check_text_view_number.isChecked) {
                try {
                    RealmBackupRestore(this).restore()
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this, R.string.error_restoring_backup, Toast.LENGTH_SHORT).show()
                } catch (e: RealmMigrationNeededException) {
                    e.printStackTrace()
                    Toast.makeText(this, R.string.error_restoring_backup, Toast.LENGTH_SHORT).show()
                } catch (e: BackupFileMismatchedException) {
                    e.printStackTrace()
                    Toast.makeText(this, R.string.backup_file_mismatched, Toast.LENGTH_SHORT).show()
                }

            }
            viewModel.completeSetup(choosenPhoto, et_username_setup.text.toString())

        }
    }


    private fun showSnackbar() {
        Snackbar.make(findViewById(android.R.id.content), R.string.no_internet_connection, Snackbar.LENGTH_SHORT).show()
    }


    private fun pickImage() {
        CropImageRequest.getCropImageRequest().start(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == Activity.RESULT_OK) {
                val resultUri = result.uri


                val file = DirManager.getMyPhotoPath()
                try {
                    //copy image to the App Folder
                    FileUtils.copyFile(resultUri.path, file)

                    Glide.with(this).load(file).into(user_img_setup!!)
                    choosenPhoto = file.path
                    progress_bar_setup_user_img!!.visibility = View.GONE
                } catch (e: IOException) {
                    e.printStackTrace()
                }


            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(this, R.string.could_not_get_this_image, Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        if (SharedPreferencesManager.isCurrentUserInfoSaved()) {
            progressDialog?.dismiss()

            startMainActivity()
        }
    }

    override fun onPause() {
        super.onPause()

        progressDialog?.dismiss()
    }
}


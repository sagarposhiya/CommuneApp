package com.devlomi.commune.activities

import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.devlomi.commune.R
import com.devlomi.commune.utils.SharedPreferencesManager
import com.devlomi.commune.utils.biometricks.BiometricException
import com.devlomi.commune.utils.biometricks.BiometricPromptInfo
import com.devlomi.commune.utils.biometricks.Biometricks
import com.devlomi.commune.utils.biometricks.Crypto
import kotlinx.android.synthetic.main.activity_lockscreen.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.InvalidAlgorithmParameterException


class LockscreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lockscreen)

        btn_retry.setOnClickListener {
            showBiometricPrompt()
        }

    }

    override fun onResume() {
        super.onResume()
        showBiometricPrompt()
    }

    private fun showBiometricPrompt() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }


        val biometricks = Biometricks.from(applicationContext)

        if (biometricks !is Biometricks.Available) {
            val string = getString(R.string.biometrics_not_available)
            tv_unlock_text.text = string
            Toast.makeText(this, string, Toast.LENGTH_SHORT).show();
            return
        }

        val biometricName = when (biometricks) {
            Biometricks.Available.Face -> getString(R.string.face)
            Biometricks.Available.Fingerprint -> getString(R.string.fingerprint)
            Biometricks.Available.Iris -> getString(R.string.iris)
            Biometricks.Available.Unknown,
            Biometricks.Available.Multiple -> getString(R.string.biometric)
        }

        btn_retry.isVisible = false



        lifecycleScope.launch {
            try {

                val cryptoObject = withContext(Dispatchers.IO) {
                    Crypto().cryptoObject()
                }


                Biometricks.showPrompt(
                        this@LockscreenActivity,
                        BiometricPromptInfo(
                                title = getString(R.string.authenticate_with, biometricName),
                                negativeButtonText = getString(R.string.cancel),
                                cryptoObject = cryptoObject
                        )
                ) { showLoading ->

                    progressBar.isVisible = showLoading


                }

                SharedPreferencesManager.setLastActive(System.currentTimeMillis())
                finish()

            } catch (e: Exception) {
                if (e is BiometricException) {

                    if (e.code == BiometricPrompt.ERROR_CANCELED || e.code == BiometricPrompt.ERROR_USER_CANCELED || e.code == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        btn_retry.isVisible = true
                    } else {
                        btn_retry.isVisible = false
                    }

                    img_unlock_icon.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)
                    tv_unlock_text.text = e.errString
                } else if (e is InvalidAlgorithmParameterException) {
                    SharedPreferencesManager.setFingerprintLock(false)
                    finish()

                }
            }
        }
    }

    override fun onBackPressed() {
        //DO NOTHING AND PREVENT EXITING
    }

}
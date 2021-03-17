package com.devlomi.commune.activities.settings

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceFragmentCompat
import com.devlomi.commune.R
import com.devlomi.commune.utils.SharedPreferencesManager
import com.devlomi.commune.utils.biometricks.BiometricPromptInfo
import com.devlomi.commune.utils.biometricks.Biometricks
import com.devlomi.commune.utils.biometricks.Crypto
import kotlinx.android.synthetic.main.activity_lockscreen.*
import kotlinx.android.synthetic.main.fragment_security.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SecurityPreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var biometricks: Biometricks

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_security, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        biometricks = Biometricks.from(requireContext().applicationContext)

        //set to default

        val isFingerPrintLockEnabled = SharedPreferencesManager.isFingerprintLockEnabled()
        setLockAfterVisibility(isFingerPrintLockEnabled)


        switch_unlock_fingerprint.isEnabled = biometricks is Biometricks.Available
        switch_unlock_fingerprint.isChecked = isFingerPrintLockEnabled

        switch_unlock_fingerprint.setOnCheckedChangeListener { compoundButton, isChecked ->
            if (isChecked) {
                showBiometricPrompt()
            } else {

                setLockAfterVisibility(false)

                if (compoundButton.isPressed) {
                    SharedPreferencesManager.setFingerprintLock(false)
                }
            }
        }

        setDefaultRadioGroupChecked()


        radio_group_lock_after.setOnCheckedChangeListener { radioGroup, id ->
            val lockAfter = when (id) {
                R.id.btn_radio_one_minute -> 1
                R.id.btn_radio_five_minutes -> 5
                R.id.btn_radio_thirty_minutes -> 30
                else -> 0 //Immediately
            }

            SharedPreferencesManager.setLockAfter(lockAfter)
        }


    }


    private fun setDefaultRadioGroupChecked() {
        val lockAfter = SharedPreferencesManager.getLockAfter()

        when (lockAfter) {
            1 -> btn_radio_one_minute.isChecked = true
            5 -> btn_radio_five_minutes.isChecked = true
            30 -> btn_radio_thirty_minutes.isChecked = true
            else -> btn_radio_immediately.isChecked = true
        }


    }


    private fun showBiometricPrompt() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }



        if (biometricks !is Biometricks.Available) {
            val string = getString(R.string.biometrics_not_available)
            tv_unlock_text.text = string
            Toast.makeText(context, string, Toast.LENGTH_SHORT).show()
            return
        }

        val biometricName = when (biometricks) {
            Biometricks.Available.Face -> getString(R.string.face)
            Biometricks.Available.Fingerprint -> getString(R.string.fingerprint)
            Biometricks.Available.Iris -> getString(R.string.iris)
            Biometricks.Available.Unknown,
            Biometricks.Available.Multiple -> getString(R.string.biometric)
            else -> ""
        }




        lifecycleScope.launch {
            try {

                val cryptoObject = withContext(Dispatchers.IO) {
                Crypto().cryptoObject()
            }

                Biometricks.showPrompt(
                        requireActivity(),
                        BiometricPromptInfo(
                                title = getString(R.string.authenticate_with, biometricName),
                                negativeButtonText = getString(R.string.cancel),
                                cryptoObject = cryptoObject
                        )
                ) { showLoading ->


                }

                SharedPreferencesManager.setFingerprintLock(true)
                setLockAfterVisibility(true)


            } catch (e: Exception) {
                switch_unlock_fingerprint.isChecked = false
                Toast.makeText(requireActivity(), R.string.could_not_add_fingerprint, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLockAfterVisibility(setVisible: Boolean) {
        tv_lock_after.isVisible = setVisible
        radio_group_lock_after.isVisible = setVisible
    }


}
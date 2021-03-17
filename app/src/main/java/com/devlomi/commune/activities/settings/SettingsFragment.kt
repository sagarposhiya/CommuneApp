package com.devlomi.commune.activities.settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.devlomi.commune.R
import kotlinx.android.synthetic.main.fragment_settings.*


class SettingsFragment : Fragment(), View.OnClickListener {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tv_profile.setOnClickListener(this)
        tv_notifications.setOnClickListener(this)
        tv_security.setOnClickListener(this)
        tv_chat.setOnClickListener(this)
        tv_privacy_policy.setOnClickListener(this)
        tv_about.setOnClickListener(this)
    }

    override fun onClick(view: View) {

        when (view.id) {

            R.id.tv_profile -> Navigation.findNavController(view).navigate(R.id.action_settingsFragment_to_profilePreferenceFragment)
            R.id.tv_notifications -> Navigation.findNavController(view).navigate(R.id.action_settingsFragment_to_notificationPreferenceFragment)
            R.id.tv_security -> Navigation.findNavController(view).navigate(R.id.action_settingsFragment_to_securityPreferencesFragment)
            R.id.tv_chat -> Navigation.findNavController(view).navigate(R.id.action_settingsFragment_to_chatSettingsPreferenceFragment2)
            R.id.tv_privacy_policy -> Navigation.findNavController(view).navigate(R.id.action_settingsFragment_to_privacyPolicyFragment)
            R.id.tv_about -> Navigation.findNavController(view).navigate(R.id.action_settingsFragment_to_aboutFragment2)


        }
    }

}
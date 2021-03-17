package com.devlomi.commune.activities.authentication

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.doOnTextChanged
import androidx.navigation.Navigation
import com.devlomi.commune.R
import com.devlomi.commune.utils.IntentUtils
import kotlinx.android.synthetic.main.fragment_verify_phone.*


class VerifyPhoneFragment : BaseAuthFragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_verify_phone, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString(IntentUtils.PHONE)?.let { phone ->
            tv_otp_info.text = requireActivity().getString(R.string.enter_the_otp_sent_to, phone)
        }

        et_otp.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 6) {
                et_otp.onEditorAction(EditorInfo.IME_ACTION_DONE)
                completeRegistration()
            }
        }


        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true /* enabled by default */) {
            override fun handleOnBackPressed() {

                AlertDialog.Builder(requireActivity()).apply {
                    setMessage(R.string.cancel_verification_confirmation_message)
                    setNegativeButton(R.string.no, null)
                    setPositiveButton(R.string.yes) { _, _ ->
                        callbacks?.cancelVerificationRequest()
                        Navigation.findNavController(et_otp).navigateUp()
                    }
                    show()
                }

            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)


    }

    private fun completeRegistration() {
        callbacks?.verifyCode(et_otp.text.toString())
    }

    override fun enableViews() {
        super.enableViews()
        et_otp.isEnabled = true
    }

    override fun disableViews() {
        super.disableViews()
        et_otp.isEnabled = false

    }
}
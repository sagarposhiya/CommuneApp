package com.devlomi.commune.activities.authentication

import android.content.Context
import androidx.fragment.app.Fragment

open class BaseAuthFragment : Fragment() {
    var callbacks: AuthCallbacks?=null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as? AuthCallbacks
    }

    open fun disableViews(){

    }

    open fun enableViews(){

    }
}
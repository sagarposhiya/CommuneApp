package com.devlomi.commune.placespicker

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PlacesPickerViewModelFactory(private val context: Context, private val lifecycleOwner: LifecycleOwner) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {

        return PlacesPickerViewModel(context, lifecycleOwner) as T
    }
}
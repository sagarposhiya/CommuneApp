package com.devlomi.commune.placespicker.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class LabeledLatLng(
        @SerializedName("label")
        val label: String,
        @SerializedName("lat")
        val lat: Double,
        @SerializedName("lng")
        val lng: Double
)
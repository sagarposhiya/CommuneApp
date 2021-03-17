package com.devlomi.commune.placespicker.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName


@Keep
data class Location(
        @SerializedName("cc")
        val cc: String,
        @SerializedName("city")
        val city: String,
        @SerializedName("country")
        val country: String,
        @SerializedName("distance")
        val distance: Int,
        @SerializedName("formattedAddress")
        val formattedAddress: List<String>,
        @SerializedName("labeledLatLngs")
        val labeledLatLngs: List<LabeledLatLng>,
        @SerializedName("lat")
        val lat: Double,
        @SerializedName("lng")
        val lng: Double,
        @SerializedName("state")
        val state: String
)
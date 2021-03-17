package com.devlomi.commune.placespicker.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class Venue(
        @SerializedName("categories")
        val categories: List<Category>,
        @SerializedName("hasPerk")
        val hasPerk: Boolean,
        @SerializedName("id")
        val id: String,
        @SerializedName("location")
        val location: Location,
        @SerializedName("name")
        val name: String,
        @SerializedName("referralId")
        val referralId: String,
        @SerializedName("venuePage")
        val venuePage: VenuePage
)
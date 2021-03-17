package com.devlomi.commune.placespicker.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class VenuePage(
        @SerializedName("id")
        val id: String
)
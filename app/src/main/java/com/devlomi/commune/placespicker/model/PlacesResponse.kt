package com.devlomi.commune.placespicker.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class PlacesResponse(
        @SerializedName("meta")
        val meta: Meta,
        @SerializedName("response")
        val response: Response
)
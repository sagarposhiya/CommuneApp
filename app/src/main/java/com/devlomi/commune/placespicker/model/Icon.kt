package com.devlomi.commune.placespicker.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class Icon(
        @SerializedName("prefix")
        val prefix: String,
        @SerializedName("suffix")
        val suffix: String
) {
    fun getIcon(size: Int) = "$prefix$size$suffix"


}


package com.devlomi.commune.adapters.messaging

import com.devlomi.commune.model.realms.Message

interface AudibleInteraction {
    fun onSeek(message:Message,progress:Int,max:Int)
}
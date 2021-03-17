package com.devlomi.commune.adapters.messaging

import com.devlomi.commune.model.realms.RealmContact

interface ContactHolderInteraction {
    fun onMessageClick(contact:RealmContact)
    fun onAddContactClick(contact:RealmContact)
}
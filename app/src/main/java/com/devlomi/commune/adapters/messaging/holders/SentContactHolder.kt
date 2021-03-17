package com.devlomi.commune.adapters.messaging.holders

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.devlomi.commune.R
import com.devlomi.commune.adapters.messaging.ContactHolderBase
import com.devlomi.commune.adapters.messaging.ContactHolderInteraction
import com.devlomi.commune.adapters.messaging.holders.base.BaseSentHolder
import com.devlomi.commune.model.realms.Message
import com.devlomi.commune.model.realms.User

class SentContactHolder(context: Context, itemView: View) : BaseSentHolder(context,itemView),ContactHolderBase {

    private val tvContactName: TextView = itemView.findViewById(R.id.tv_contact_name)
    private val btnMessageContact: Button = itemView.findViewById(R.id.btn_message_contact)

    override var contactHolderInteraction: ContactHolderInteraction? = null

    override fun bind(message: Message,user: User) {
        super.bind(message,user)
        //set contact name
        tvContactName.text = message.content


        //send a message to this contact if installed this app
        btnMessageContact.setOnClickListener {
            contactHolderInteraction?.onMessageClick(message.contact)
        }

    }



}


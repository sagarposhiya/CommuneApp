package com.devlomi.commune.adapters.messaging.holders

import android.content.Context
import android.view.View
import com.devlomi.commune.R
import com.devlomi.commune.adapters.messaging.holders.base.BaseReceivedHolder
import com.devlomi.commune.model.realms.Message
import com.devlomi.commune.model.realms.User
import com.vanniktech.emoji.EmojiTextView

// received message with type text
class ReceivedTextHolder(context: Context, itemView: View) : BaseReceivedHolder(context,itemView) {

    private var tvMessageContent: EmojiTextView = itemView.findViewById(R.id.tv_message_content)

    override fun bind(message: Message,user: User) {
        super.bind(message,user)
        tvMessageContent.text = message.content
    }


}
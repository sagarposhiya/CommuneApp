package com.devlomi.commune.adapters.messaging.holders

import android.content.Context
import android.view.View
import com.devlomi.commune.R
import com.devlomi.commune.adapters.messaging.holders.base.BaseSentHolder
import com.devlomi.commune.model.realms.Message
import com.devlomi.commune.model.realms.User
import com.vanniktech.emoji.EmojiTextView
import com.vanniktech.emoji.EmojiUtils


// sent message with type text
class SentTextHolder(context: Context, itemView: View) : BaseSentHolder(context,itemView) {
    private var tvMessageContent: EmojiTextView = itemView.findViewById(R.id.tv_message_content)

    override fun bind(message: Message, user: User) {
        super.bind(message,user)

        val emojiInformation = EmojiUtils.emojiInformation(message.content)
        val res: Int

        res = if (emojiInformation.isOnlyEmojis && emojiInformation.emojis.size == 1) {
            R.dimen.emoji_size_single_emoji
        } else if (emojiInformation.isOnlyEmojis && emojiInformation.emojis.size > 1) {
            R.dimen.emoji_size_only_emojis
        } else {
            R.dimen.emoji_size_default
        }

        tvMessageContent.setEmojiSizeRes(res, false)
        tvMessageContent.text = message.content
    }

}


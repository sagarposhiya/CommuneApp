package com.devlomi.commune.adapters.messaging.holders.base

import android.content.Context
import android.view.View
import android.widget.ImageView
import com.devlomi.commune.R
import com.devlomi.commune.model.realms.Message
import com.devlomi.commune.model.realms.User
import com.devlomi.commune.utils.AdapterHelper

open class BaseSentHolder(context: Context, itemView: View) : BaseHolder(context,itemView) {

    var messageStatImg:ImageView? = itemView.findViewById(R.id.message_stat_img)


    override fun bind(message: Message, user: User) {
        super.bind(message, user)


        //imgStat (received or read)
        messageStatImg?.setImageResource(AdapterHelper.getMessageStatDrawable(message.messageStat))


    }




}


package com.devlomi.commune.adapters.messaging.holders

import android.content.Context
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.devlomi.commune.R
import com.devlomi.commune.adapters.messaging.holders.base.BaseSentHolder
import com.devlomi.commune.model.constants.DownloadUploadStat
import com.devlomi.commune.model.realms.Message
import com.devlomi.commune.model.realms.User


class SentVideoMessageHolder(context: Context, itemView: View) : BaseSentHolder(context,itemView) {

    private val thumbImg: ImageView = itemView.findViewById(R.id.thumb_img)
    private val btnPlayVideo: ImageButton = itemView.findViewById(R.id.btn_play_video)

    private val tvMediaDuration: TextView = itemView.findViewById(R.id.tv_media_duration)

    override fun bind(message: Message, user: User) {
        super.bind(message,user)
        if (message.downloadUploadStat != DownloadUploadStat.SUCCESS) {
            tvMediaDuration.visibility = View.GONE
        } else {
            tvMediaDuration.visibility = View.VISIBLE
            tvMediaDuration.text = message.mediaDuration
        }



        Glide.with(context).load(message.videoThumb).into(thumbImg)

        btnPlayVideo?.setOnClickListener { interaction?.onContainerViewClick(adapterPosition, itemView, message) }

    }


}

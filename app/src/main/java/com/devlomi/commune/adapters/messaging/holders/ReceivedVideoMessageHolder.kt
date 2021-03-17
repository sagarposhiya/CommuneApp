package com.devlomi.commune.adapters.messaging.holders

import android.content.Context
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.devlomi.commune.R
import com.devlomi.commune.adapters.messaging.holders.base.BaseReceivedHolder
import com.devlomi.commune.model.constants.DownloadUploadStat.SUCCESS
import com.devlomi.commune.model.realms.Message
import com.devlomi.commune.model.realms.User
import com.devlomi.commune.utils.FileUtils

class ReceivedVideoMessageHolder(context: Context, itemView: View) : BaseReceivedHolder(context, itemView) {

    private val thumbImg: ImageView = itemView.findViewById(R.id.thumb_img)
    private val btnPlayVideo: ImageButton = itemView.findViewById(R.id.btn_play_video)

    private val tvMediaDuration: TextView = itemView.findViewById(R.id.tv_media_duration)

    override fun bind(message: Message, user: User) {
        super.bind(message, user)


        //set duration
        tvMediaDuration.text = message.mediaDuration

        //Video is not downloaded yet
        //show the blurred thumb
        if (message.localPath == null) {
            Glide.with(context).load(message.thumb).into(thumbImg)

        } else {

            //if it's downloaded but the user deleted the file from device
            if (!FileUtils.isFileExists(message.localPath)) {
                //show the blurred image
                Glide.with(context).load(message.thumb).into(thumbImg)
            } else {
                //if it's downloaded ,show the Video Thumb (Without blur)
                Glide.with(context).load(message.videoThumb).into(thumbImg)
            }
        }

        btnPlayVideo.visibility = if (message.downloadUploadStat == SUCCESS) View.VISIBLE else View.GONE
        btnPlayVideo?.setOnClickListener { interaction?.onContainerViewClick(adapterPosition, itemView, message) }

    }


}
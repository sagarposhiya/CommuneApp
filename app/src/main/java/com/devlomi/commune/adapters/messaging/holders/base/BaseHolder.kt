package com.devlomi.commune.adapters.messaging.holders.base

import android.content.Context
import android.graphics.PorterDuff
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.devlomi.commune.R
import com.devlomi.commune.adapters.messaging.Interaction
import com.devlomi.commune.common.extensions.tint
import com.devlomi.commune.model.constants.DownloadUploadStat
import com.devlomi.commune.model.constants.MessageType
import com.devlomi.commune.model.realms.Message
import com.devlomi.commune.model.realms.QuotedMessage
import com.devlomi.commune.model.realms.User
import com.devlomi.commune.utils.ListUtil
import com.devlomi.commune.utils.MessageTypeHelper
import com.devlomi.commune.utils.MyApp
import com.devlomi.commune.utils.network.FireManager
import com.github.abdularis.buttonprogress.DownloadButtonProgress
import com.vanniktech.emoji.EmojiTextView


open class BaseHolder(val context: Context, itemView: View) : RecyclerView.ViewHolder(itemView) {
    var tvTime: TextView? = itemView.findViewById(R.id.tv_time)
    var container: View? = itemView.findViewById(R.id.container)

    //Quoted MessageLayouts
    private val quotedMessageFrame: FrameLayout? = itemView.findViewById(R.id.quoted_message_frame)
    private val quotedColor: View? = itemView.findViewById(R.id.quoted_color)
    private val tvQuotedName: EmojiTextView? = itemView.findViewById(R.id.tv_quoted_name)
    private val tvQuotedText: EmojiTextView? = itemView.findViewById(R.id.tv_quoted_text)
    private val quotedThumb: ImageView? = itemView.findViewById(R.id.quoted_thumb)

    var tvSize: TextView? = itemView.findViewById(R.id.tv_size)
    var progressButton: DownloadButtonProgress? = itemView.findViewById(R.id.progress_button)


    var interaction: Interaction? = null

    //to keep track of selected Items
    var lifecycleOwner: LifecycleOwner? = null

    var progressMap: LiveData<Map<String, Int>>? = null

    var selectedItems: LiveData<List<Message>>? = null
    open fun bind(message: Message, user: User) {


        tvTime?.text = message.time
        tvSize?.text = message.metadata

        if (quotedMessageFrame != null) {
            if (message.quotedMessage == null) {
                quotedMessageFrame.visibility = View.GONE
            } else {
                val quotedMessage = QuotedMessage.quotedMessageToMessage(message.quotedMessage)
//p                quotedMessageFrame.setBackgroundColor(ContextCompat.getColor(context, R.color.quoted_received_background_color))
                tvQuotedName?.setTextColor(ContextCompat.getColor(context, R.color.quoted_received_text_color))
                tvQuotedText?.setTextColor(ContextCompat.getColor(context, R.color.colorText))
                quotedColor?.setBackgroundColor(ContextCompat.getColor(context, R.color.quoted_received_quoted_color))
                quotedMessageFrame.visibility = View.VISIBLE
                tvQuotedName?.text = getQuotedUsername(quotedMessage, user)
                tvQuotedText?.text = MessageTypeHelper.getMessageContent(quotedMessage, false)
                setQuotedTextDrawable(quotedMessage)
                if (quotedMessage.thumb != null) {
                    quotedThumb?.visibility = View.VISIBLE
                    if (quotedThumb != null)
                        Glide.with(context).load(quotedMessage.thumb).into(quotedThumb)
                } else quotedThumb?.visibility = View.GONE

                quotedMessageFrame.setOnClickListener(View.OnClickListener {
                    interaction?.onQuotedMessageClick(adapterPosition, itemView, message)
                })
            }
        }

        val progressIdleIconRes = if (MessageType.isSentType(message.type)) R.drawable.ic_file_upload else R.drawable.ic_file_download
        val progressIdleIcon = AppCompatResources.getDrawable(context, progressIdleIconRes)!!
        progressIdleIcon.tint(context, R.color.white)
        progressButton?.idleIcon = progressIdleIcon

        hideOrShowProgressView(progressButton, tvSize, message.downloadUploadStat)

        itemView.setOnClickListener {
            interaction?.onItemViewClick(adapterPosition, itemView, message)
        }

        container?.setOnClickListener {
            interaction?.onContainerViewClick(adapterPosition, itemView, message)
        }


        itemView.setOnLongClickListener {
            interaction?.onLongClick(adapterPosition, itemView, message)
            true
        }

        container?.setOnLongClickListener {
            interaction?.onLongClick(adapterPosition, itemView, message)
            true
        }

        progressButton?.setOnClickListener {
            interaction?.onProgressButtonClick(adapterPosition, itemView, message)
        }


        lifecycleOwner?.let {
            selectedItems?.observe(it, Observer { selectedMessages ->
                var contains = selectedMessages.contains(message)
                setBackgroundColor(itemView, contains)
            })

            progressMap?.observe(it, Observer { progressHashmap ->
                if (progressHashmap.containsKey(message.messageId) && message.downloadUploadStat == DownloadUploadStat.LOADING && message.downloadUploadStat == DownloadUploadStat.LOADING) {
                    progressHashmap[message.messageId]?.let { progress ->
                        progressButton?.visibility = View.VISIBLE
                        progressButton?.currentProgress = progress
                    }

                }
            })
        }

    }

    //set background color of item if it's selected
    private fun setBackgroundColor(view: View, isAdded: Boolean) {
        val addedColor = context.resources.getColor(R.color.item_selected_background_color)
        val notAddedColor = 0x00000000
        if (isAdded) view.setBackgroundColor(addedColor) else view.setBackgroundColor(notAddedColor)
    }

    private fun setQuotedTextDrawable(quotedMessage: Message) {
        if (!quotedMessage.isTextMessage && MessageTypeHelper.getMessageTypeDrawable(quotedMessage.type) != -1) {
            val drawable = context.resources
                    .getDrawable(MessageTypeHelper.getMessageTypeDrawable(quotedMessage.type))
            drawable.mutate().setColorFilter(ContextCompat.getColor(context, R.color.grey), PorterDuff.Mode.SRC_IN)
            tvQuotedText?.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
        } else tvQuotedText?.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
    }

    private fun getQuotedUsername(quotedMessage: Message, user: User?): String {
        var userName = ""
        val fromId = quotedMessage.fromId
        if (fromId == FireManager.uid) userName = context.resources.getString(R.string.you) else {
            val groupBool = user?.isGroupBool ?: false
            if (groupBool && user?.group != null && user?.group.users != null) {
                val user = ListUtil.getUserById(fromId, user?.group.users)
                if (user != null) {
                    userName = user.properUserName
                }
            } else {
                userName = user?.userName ?: ""
            }
        }
        return if (quotedMessage.status != null) {
            userName + " • " + MyApp.context().getString(R.string.status)
        } else {
            userName
        }
    }


}


    // hide or show some views depending on download/upload state
    private fun hideOrShowProgressView(progressButton: DownloadButtonProgress?, tvSize: TextView?, stat: Int) {
        if (progressButton == null) return
        when (stat) {
            DownloadUploadStat.FAILED, DownloadUploadStat.CANCELLED -> {
                progressButton.visibility = View.VISIBLE
                tvSize?.visibility = View.VISIBLE

                progressButton?.setIdle()
            }
            DownloadUploadStat.LOADING -> {
                progressButton.visibility = View.VISIBLE
                tvSize?.visibility = View.GONE
                progressButton?.setDeterminate()
//                progressLayout.visibility = View.VISIBLE
//                btnRetry.visibility = View.GONE
            }
            DownloadUploadStat.SUCCESS -> {
                progressButton?.setFinish()
                progressButton.visibility = View.GONE
                tvSize?.visibility = View.GONE
//                progressLayout.visibility = View.GONE
//                btnRetry.visibility = View.GONE
            }
        }
    }



package com.devlomi.commune.adapters.messaging

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import ca.barrenechea.widget.recyclerview.decoration.StickyHeaderAdapter
import com.devlomi.commune.R
import com.devlomi.commune.adapters.messaging.holders.*
import com.devlomi.commune.adapters.messaging.holders.base.BaseHolder
import com.devlomi.commune.adapters.messaging.holders.base.ReceivedDeletedMessageHolder
import com.devlomi.commune.model.AudibleState
import com.devlomi.commune.model.constants.MessageType
import com.devlomi.commune.model.realms.Message
import com.devlomi.commune.model.realms.User
import com.devlomi.commune.utils.TimeHelper
import io.realm.OrderedRealmCollection
import io.realm.RealmRecyclerViewAdapter
import java.util.*

/**
 * Created by Devlomi on 07/08/2017.
 */
//the RealmRecyclerViewAdapter provides autoUpdate feature
//which will handle changes in list automatically with smooth animations
class MessagingAdapter(private val messages: OrderedRealmCollection<Message>, autoUpdate: Boolean,
                       private val context: Context, private val lifecycleOwner: LifecycleOwner, var user: User, private val myThumbImg: String,
                       private val selectedItems: LiveData<List<Message>>,
                       private val progressMap: LiveData<Map<String, Int>>, private val audibleState: LiveData<Map<String, AudibleState>>)

    : RealmRecyclerViewAdapter<Message, RecyclerView.ViewHolder>(messages, autoUpdate)
        , StickyHeaderAdapter<RecyclerView.ViewHolder> {

    private val interaction = context as? Interaction?
    private val contactHolderInteraction = context as? ContactHolderInteraction?
    private val audibleHolderInteraction = context as? AudibleInteraction?

    //timestamps to implement the date header
    var timestamps = HashMap<Int, Long>()
    var lastTimestampPos = 0


    //date header
    override fun getHeaderId(position: Int): Long {
        return if (timestamps.containsKey(position)) {
            timestamps[position] ?: 0
        } else 0
    }

    //date header
    override fun onCreateHeaderViewHolder(parent: ViewGroup): RecyclerView.ViewHolder? {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_day, parent, false)
        return HeaderHolder(view)
    }

    //date header
    override fun onBindHeaderViewHolder(viewholder: RecyclerView.ViewHolder?, position: Int) {
        val mHolder = viewholder as HeaderHolder?

        //if there are no timestamps in this day then hide the header
        //otherwise show it
        val headerId = getHeaderId(position)
        if (headerId == 0L) mHolder?.header?.visibility = View.GONE else {
            val formatted = TimeHelper.getChatTime(headerId)
            mHolder?.header?.text = formatted
        }
    }

    override fun getItemCount() = messages.size


    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return message.type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // check the type of view and return holder
        return getHolderByType(parent, viewType)
    }

    override fun onBindViewHolder(mHolder: RecyclerView.ViewHolder, position: Int) {

        //get itemView type
        val type = getItemViewType(position)
        val message = messages[position]



        when (type) {
            MessageType.SENT_TEXT -> {
                val sentTextHolder = mHolder as SentTextHolder
                initHolder(sentTextHolder)
                sentTextHolder.bind(message, user)
            }
            MessageType.SENT_IMAGE -> {
                val sentImageHolder = mHolder as SentImageHolder
                initHolder(sentImageHolder)
                sentImageHolder.bind(message, user)
            }
            MessageType.SENT_VOICE_MESSAGE -> {
                val sentVoiceMessageHolder = mHolder as SentVoiceMessageHolder
                initHolder(sentVoiceMessageHolder)
                initAudibleHolder(sentVoiceMessageHolder)
                sentVoiceMessageHolder.bind(message, user)
            }
            MessageType.SENT_VIDEO -> {
                val sentVideoMessageHolder = mHolder as SentVideoMessageHolder
                initHolder(sentVideoMessageHolder)
                sentVideoMessageHolder.bind(message, user)
            }
            MessageType.SENT_FILE -> {
                val sentFileHolder = mHolder as SentFileHolder
                initHolder(sentFileHolder)
                sentFileHolder.bind(message, user)
            }
            MessageType.SENT_AUDIO -> {
                val sentAudioHolder = mHolder as SentAudioHolder
                initHolder(sentAudioHolder)
                initAudibleHolder(sentAudioHolder)
                sentAudioHolder.bind(message, user)
            }
            MessageType.SENT_CONTACT -> {
                val sentContactHolder = mHolder as SentContactHolder
                initHolder(sentContactHolder)
                initContactHolder(sentContactHolder)
                sentContactHolder.bind(message, user)
            }

            MessageType.SENT_LOCATION -> {
                val sentLocationHolder = mHolder as SentLocationHolder
                initHolder(sentLocationHolder)
                sentLocationHolder.bind(message, user)
            }
            MessageType.RECEIVED_TEXT -> {
                val holder = mHolder as ReceivedTextHolder
                initHolder(holder)
                holder.bind(message, user)
            }
            MessageType.RECEIVED_IMAGE -> {
                val receivedImageHolder = mHolder as ReceivedImageHolder
                initHolder(receivedImageHolder)
                receivedImageHolder.bind(message, user)
            }
            MessageType.RECEIVED_VOICE_MESSAGE -> {
                val receivedVoiceMessageHolder = mHolder as ReceivedVoiceMessageHolder
                initHolder(receivedVoiceMessageHolder)
                initAudibleHolder(receivedVoiceMessageHolder)
                receivedVoiceMessageHolder.bind(message, user)
            }
            MessageType.RECEIVED_VIDEO -> {
                val receivedVideoMessageHolder = mHolder as ReceivedVideoMessageHolder
                initHolder(receivedVideoMessageHolder)
                receivedVideoMessageHolder.bind(message, user)
            }
            MessageType.RECEIVED_FILE -> {
                val receivedFileHolder = mHolder as ReceivedFileHolder
                initHolder(receivedFileHolder)
                receivedFileHolder.bind(message, user)
            }
            MessageType.RECEIVED_AUDIO -> {
                val receivedAudioHolder = mHolder as ReceivedAudioHolder
                initHolder(receivedAudioHolder)
                initAudibleHolder(receivedAudioHolder)
                receivedAudioHolder.bind(message, user)
            }
            MessageType.RECEIVED_CONTACT -> {
                val receivedContactHolder = mHolder as ReceivedContactHolder
                initHolder(receivedContactHolder)
                initContactHolder(receivedContactHolder)
                receivedContactHolder.bind(message, user)
            }
            MessageType.RECEIVED_LOCATION -> {
                val receivedLocationHolder = mHolder as ReceivedLocationHolder
                initHolder(receivedLocationHolder)
                receivedLocationHolder.bind(message, user)
            }
            MessageType.SENT_DELETED_MESSAGE -> {
                val sentDeletedMessageHolder = mHolder as SentDeletedMessageHolder
                sentDeletedMessageHolder.bind(message, user)
            }
            MessageType.RECEIVED_DELETED_MESSAGE -> {
                val receivedDeletedMessageHolder = mHolder as ReceivedDeletedMessageHolder
                receivedDeletedMessageHolder.bind(message, user)
            }
            MessageType.GROUP_EVENT -> {
                val groupEventHolder = mHolder as GroupEventHolder
                groupEventHolder.bind(message, user)
            }
            else -> {
                val notSupportedTypeHolder = mHolder as? NotSupportedTypeHolder
                notSupportedTypeHolder?.bind(message,user)
            }
        }
    }

    private fun initHolder(baseHolder: BaseHolder) {
        baseHolder.selectedItems = selectedItems
        baseHolder.progressMap = progressMap
        baseHolder.lifecycleOwner = lifecycleOwner
        baseHolder.interaction = interaction
    }

    private fun initAudibleHolder(audibleBase: AudibleBase) {
        audibleBase.audibleInteraction = audibleHolderInteraction
        audibleBase.audibleState = audibleState
    }

    private fun initContactHolder(contactHolderBase: ContactHolderBase) {
        contactHolderBase.contactHolderInteraction = contactHolderInteraction
    }


    private fun distinctMessagesTimestamps() {

        for (i in messages.indices) {
            val timestamp = messages[i].timestamp.toLong()
            if (i == 0) {
                timestamps[i] = timestamp
                lastTimestampPos = i
            } else {
                val oldTimestamp = messages[i - 1].timestamp.toLong()
                if (!TimeHelper.isSameDay(timestamp, oldTimestamp)) {
                    timestamps[i] = timestamp
                    lastTimestampPos = i
                }
            }

        }

    }

    //update timestamps if needed when a new message inserted
    fun messageInserted() {
        val index = messages.size - 1
        val newTimestamp = messages[index].timestamp.toLong()
        if (timestamps.isEmpty()) {
            timestamps[index] = newTimestamp
            lastTimestampPos = index
            return
        }
        val lastTimestamp = timestamps[lastTimestampPos]!!
        if (!TimeHelper.isSameDay(lastTimestamp, newTimestamp)) {
            timestamps[index] = newTimestamp
            lastTimestampPos = index
        }
    }


    private fun getHolderByType(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            MessageType.DAY_ROW -> return TimestampHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_day, parent, false))
            MessageType.SENT_DELETED_MESSAGE -> return SentDeletedMessageHolder(context, LayoutInflater.from(parent.context).inflate(R.layout.row_sent_deleted_message, parent, false))
            MessageType.RECEIVED_DELETED_MESSAGE -> return ReceivedDeletedMessageHolder(context, LayoutInflater.from(parent.context).inflate(R.layout.row_received_deleted_message, parent, false))
            MessageType.SENT_TEXT -> return SentTextHolder(context, LayoutInflater.from(parent.context).inflate(R.layout.row_sent_message_text, parent, false))
            MessageType.SENT_IMAGE -> return SentImageHolder(context, LayoutInflater.from(parent.context).inflate(R.layout.row_sent_message_img, parent, false))
            MessageType.RECEIVED_TEXT -> return ReceivedTextHolder(context, LayoutInflater.from(parent.context).inflate(R.layout.row_received_message_text, parent, false))
            MessageType.RECEIVED_IMAGE -> return ReceivedImageHolder(context, LayoutInflater.from(parent.context).inflate(R.layout.row_received_message_img, parent, false))
            MessageType.SENT_VOICE_MESSAGE -> return SentVoiceMessageHolder(context, LayoutInflater.from(parent.context).inflate(R.layout.row_sent_voice_message, parent, false), myThumbImg)
            MessageType.RECEIVED_VOICE_MESSAGE -> return ReceivedVoiceMessageHolder(context, LayoutInflater.from(parent.context).inflate(R.layout.row_received_message_voice, parent, false))
            MessageType.RECEIVED_VIDEO -> return ReceivedVideoMessageHolder(context, LayoutInflater.from(parent.context).inflate(R.layout.row_received_message_video, parent, false))
            MessageType.SENT_VIDEO -> return SentVideoMessageHolder(context, LayoutInflater.from(parent.context).inflate(R.layout.row_sent_message_video, parent, false))
            MessageType.SENT_FILE -> return SentFileHolder(context, LayoutInflater.from(parent.context).inflate(R.layout.row_sent_file, parent, false))
            MessageType.RECEIVED_FILE -> return ReceivedFileHolder(context, LayoutInflater.from(parent.context).inflate(R.layout.row_received_file, parent, false))
            MessageType.SENT_AUDIO -> return SentAudioHolder(context, LayoutInflater.from(parent.context).inflate(R.layout.row_sent_audio, parent, false))
            MessageType.RECEIVED_AUDIO -> return ReceivedAudioHolder(context, LayoutInflater.from(parent.context).inflate(R.layout.row_received_audio, parent, false))
            MessageType.SENT_CONTACT -> return SentContactHolder(context, LayoutInflater.from(parent.context).inflate(R.layout.row_sent_contact, parent, false))
            MessageType.RECEIVED_CONTACT -> return ReceivedContactHolder(context, LayoutInflater.from(parent.context).inflate(R.layout.row_received_contact, parent, false))
            MessageType.SENT_LOCATION -> return SentLocationHolder(context, LayoutInflater.from(parent.context).inflate(R.layout.row_sent_location, parent, false))
            MessageType.RECEIVED_LOCATION -> return ReceivedLocationHolder(context, LayoutInflater.from(parent.context).inflate(R.layout.row_received_location, parent, false))
            MessageType.GROUP_EVENT -> return GroupEventHolder(context, LayoutInflater.from(parent.context).inflate(R.layout.row_group_event, parent, false))
        }
        return NotSupportedTypeHolder(context, LayoutInflater.from(parent.context).inflate(R.layout.row_not_supported, parent, false))
    }


    init {
        distinctMessagesTimestamps()
    }

}
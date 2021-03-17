package com.devlomi.commune.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.devlomi.commune.R
import com.devlomi.commune.model.realms.StatusSeenBy
import com.devlomi.commune.model.realms.User
import com.devlomi.commune.utils.TimeHelper
import io.realm.RealmRecyclerViewAdapter
import io.realm.RealmResults
import kotlinx.android.synthetic.main.row_seen_by.view.*

class StatusSeenByAdapter(private val seenByList: RealmResults<StatusSeenBy>, callback: StatusSeenByCallback)
    : RealmRecyclerViewAdapter<StatusSeenBy, StatusSeenByAdapter.SeenByHolder>(seenByList, true) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeenByHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_seen_by, parent, false)
        return SeenByHolder(view)
    }

    override fun getItemCount() = seenByList.size

    override fun onBindViewHolder(holder: SeenByHolder, position: Int) {
        val user = seenByList[position]
        if (user != null)
            holder.bind(user)
    }

    inner class SeenByHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(seenByUser: StatusSeenBy) {
            val user = seenByUser.user


            val seenAt = seenByUser.seenAt
            val date = TimeHelper.getTimeAgo(seenAt)
            val timestamp = when {
                //if there are users on old version the timestamp will not be shown
                seenAt == 0L -> ""
                TimeHelper.getTimeAgo(seenAt) == "" -> itemView.context.resources.getString(R.string.now)
                else -> {
                    TimeHelper.getTimeAgo(seenAt)
                }
            }

            itemView.tv_username.text = user?.userName
            Glide.with(itemView.context).load(user?.thumbImg).into(itemView.user_img)
            itemView.tv_seen_time.text = timestamp
        }
    }
}

interface StatusSeenByCallback {
    fun onClick(user: User, itemView: View)
}
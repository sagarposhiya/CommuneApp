package com.devlomi.commune.activities

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devlomi.commune.R
import com.devlomi.commune.activities.main.messaging.ChatActivity
import com.devlomi.commune.adapters.ForwardAdapter
import com.devlomi.commune.adapters.NewGroupAdapter
import com.devlomi.commune.adapters.NewGroupSelectedUsersAdapter
import com.devlomi.commune.model.realms.User
import com.devlomi.commune.utils.IntentUtils
import com.devlomi.commune.utils.NetworkHelper
import com.devlomi.commune.utils.RealmHelper
import com.devlomi.commune.utils.network.BroadcastManager

import com.devlomi.commune.utils.network.GroupManager
import com.devlomi.commune.views.dialogs.SetGroupTitleDialog
import com.devlomi.hidely.hidelyviews.HidelyImageView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.reactivex.rxkotlin.addTo
import io.realm.RealmResults
import java.util.*

class NewGroupActivity : ForwardActivity(), ForwardAdapter.OnUserClick, NewGroupSelectedUsersAdapter.OnUserClick, ForwardActivity.SearchCallback {
    private var EXTRA_COUNT = 0
    private var toolbarForward: Toolbar? = null
    private lateinit var rvSelectedUsersNewGroup: RecyclerView
    private lateinit var rvGroup: RecyclerView
    private lateinit var fabNext: FloatingActionButton

    //all users adapter(vertical list)
    lateinit var allUsersAdapter: NewGroupAdapter

    //selected users adapter(horizontal selected list)
    lateinit var selectedUsersAdapter: NewGroupSelectedUsersAdapter

    //all users list
    var users: RealmResults<User>? = null

    //selected users list (horizontal list)
    lateinit var selectedUsers: MutableList<User>

    //this will instantiated when adding a user to existing group
    //so we can prevent to add an existing user
    var currentUsers: List<User>? = null
    private lateinit var tvAddParticipantsTvToolbar: TextView
    private lateinit var toolbarTitle: TextView
    private var isBroadcast = false
    private val groupManager = GroupManager()
    private val broadcastManager = BroadcastManager()
    override fun onQuery(newText: String) {
        if (newText.trim().isNotEmpty()) {
            val users = RealmHelper.getInstance().searchForUser(newText, false)
            allUsersAdapter = NewGroupAdapter(users, selectedForwardedUsers, currentUsers, isBroadcast, true, this, this)
            rvGroup.adapter = allUsersAdapter
        } else {
            allUsersAdapter = NewGroupAdapter(users, selectedForwardedUsers, currentUsers, isBroadcast, true, this, this)
            rvGroup.adapter = allUsersAdapter
        }
    }

    override fun onSearchClose() {
        allUsersAdapter = NewGroupAdapter(users, selectedForwardedUsers, currentUsers, isBroadcast, true, this, this)
        rvGroup.adapter = allUsersAdapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_group)
        init()
        supportActionBar?.title = ""
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val groupId = intent.getStringExtra(IntentUtils.UID)
        isBroadcast = intent.getBooleanExtra(IntentUtils.IS_BROADCAST, false)
        if (isBroadcast) {
            //check if there are existing users in Broadcast
            if (groupId != null) {
                toolbarTitle.text = resources.getString(R.string.add_recipients)
                toolbarTitle.setText(R.string.add_recipients)
                val user = RealmHelper.getInstance().getUser(groupId)
                currentUsers = user.broadcast.users
                EXTRA_COUNT = currentUsers?.size ?: 0
                tvAddParticipantsTvToolbar.visibility = View.GONE
            } else {
                toolbarTitle.text = resources.getString(R.string.new_broadcast)
            }
        } else if (groupId != null) {
            val user = RealmHelper.getInstance().getUser(groupId)
            currentUsers = user.group.users
            EXTRA_COUNT = currentUsers?.size ?: 0
            toolbarTitle.setText(R.string.add_participants)
            tvAddParticipantsTvToolbar.visibility = View.GONE
        }
        setAdapter()
        fabNext.setOnClickListener {
            if (intent.hasExtra(IntentUtils.UID)) {
                val data = Intent()
                data.putExtra(IntentUtils.EXTRA_SELECTED_USERS, selectedUsers as ArrayList<out Parcelable?>?)
                setResult(Activity.RESULT_OK, data)
                finish()
            } else {
                val dialog = SetGroupTitleDialog(this@NewGroupActivity, "")
                if (isBroadcast) {
                    dialog.setDialogTitle(resources.getString(R.string.broadcast_name))
                    dialog.setEditTextHint(resources.getString(R.string.broadcast_name))
                }
                dialog.setmListener { groupTitle -> if (isBroadcast) createBroadcast(groupTitle) else createGroup(groupTitle) }
                dialog.show()
            }
        }
        setSearchCallback(this)
    }

    private fun createBroadcast(broadcastName: String) {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage(resources.getString(R.string.loading))
        progressDialog.setCancelable(false)
        progressDialog.show()
        if (NetworkHelper.isConnected(this)) {
            broadcastManager.createNewBroadcast(broadcastName, selectedUsers.toList()).subscribe { broadcastUser, throwable ->
                progressDialog.dismiss()

                if (throwable != null) {

                    Toast.makeText(this@NewGroupActivity, R.string.error, Toast.LENGTH_SHORT).show()

                } else {

                    val intent = Intent(this@NewGroupActivity, ChatActivity::class.java)
                    intent.putExtra(IntentUtils.UID, broadcastUser.uid)
                    startActivity(intent)
                    finish()
                }

            }.addTo(disposables)
        } else {
            progressDialog.dismiss()
            Toast.makeText(this, R.string.no_internet_connection, Toast.LENGTH_SHORT).show()
        }
    }

    private fun createGroup(groupTitle: String) {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage(resources.getString(R.string.loading))
        progressDialog.setCancelable(false)
        progressDialog.show()
        if (NetworkHelper.isConnected(this)) {
            groupManager.createNewGroup(groupTitle, selectedUsers).subscribe { groupUser, throwable ->
                progressDialog.dismiss()

                if (throwable != null) {
                    Toast.makeText(this@NewGroupActivity, R.string.error, Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(this@NewGroupActivity, ChatActivity::class.java)
                    intent.putExtra(IntentUtils.UID, groupUser.uid)
                    startActivity(intent)
                    finish()
                }
            }.addTo(disposables)


        } else {
            progressDialog.dismiss()
            Toast.makeText(this, R.string.no_internet_connection, Toast.LENGTH_SHORT).show()
        }
    }

    private fun init() {
        toolbarForward = findViewById(R.id.toolbar_forward)
        rvSelectedUsersNewGroup = findViewById(R.id.rv_selected_users_new_group)
        rvGroup = findViewById(R.id.rv_group)
        fabNext = findViewById(R.id.fab_next)
        toolbarTitle = findViewById(R.id.toolbar_title)
        tvAddParticipantsTvToolbar = findViewById(R.id.tv_add_participants_tv_toolbar)
        setSupportActionBar(toolbarForward)
        users = RealmHelper.getInstance().listOfUsers
        selectedUsers = ArrayList()
    }

    private fun setAdapter() {
        allUsersAdapter = NewGroupAdapter(users, selectedForwardedUsers, currentUsers, isBroadcast, true, this, this)
        rvGroup.layoutManager = LinearLayoutManager(this)
        rvGroup.adapter = allUsersAdapter
        selectedUsersAdapter = NewGroupSelectedUsersAdapter(selectedUsers, this, this)
        rvSelectedUsersNewGroup.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvSelectedUsersNewGroup.adapter = selectedUsersAdapter
    }

    //this will called when a user clicks on a user in vertical list
    override fun onChange(user: User, added: Boolean) {
        val count = selectedForwardedUsers.size
        val position = selectedUsers.indexOf(user)
        if (added) {
            if (count + EXTRA_COUNT > maxNumberUsers) {
                Toast.makeText(this, R.string.max_number_of_users_reached, Toast.LENGTH_SHORT).show()
            } else {
                selectedUsers.add(user)
                selectedUsersAdapter.notifyItemInserted(position)
            }
        } else {
            selectedUsers.remove(user)
            selectedUsersAdapter.notifyItemRemoved(position)
        }
        updateSelectedUsers(count)
    }

    private val maxNumberUsers: Int
        private get() = if (isBroadcast) resources.getInteger(R.integer.max_broadcast_users_count) else resources.getInteger(R.integer.max_group_users_count)

    //this will called when a user clicks on a user in horizontal list
    override fun onRemove(user: User) {
        //remove selected circle
        val viewHolderForAdapterPosition = rvGroup.findViewHolderForAdapterPosition(users?.indexOf(user)
                ?: 0)
        val selectedCircle = viewHolderForAdapterPosition?.itemView?.findViewById<HidelyImageView>(R.id.img_selected)
        selectedCircle?.hide()
        selectedForwardedUsers.remove(user)
        onChange(user, false)
    }

    private fun updateSelectedUsers(count: Int) {
        if (count == 0) {
            tvAddParticipantsTvToolbar.text = resources.getString(R.string.add_participants)
            rvSelectedUsersNewGroup.visibility = View.GONE
            if (fabNext.visibility == View.VISIBLE) fabNext.hide()
        } else {
            if (fabNext.visibility != View.VISIBLE) fabNext.show()
            if (rvSelectedUsersNewGroup.visibility != View.VISIBLE) {
                rvSelectedUsersNewGroup.visibility = View.VISIBLE
            }
            tvAddParticipantsTvToolbar.text = "$count of $maxNumberUsers"
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) onBackPressed()
        return super.onOptionsItemSelected(item)
    }


}
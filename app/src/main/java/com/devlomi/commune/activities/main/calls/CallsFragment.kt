package com.devlomi.commune.activities.main.calls

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.devlomi.commune.R
import com.devlomi.commune.activities.main.MainViewModel
import com.devlomi.commune.adapters.CallsAdapter
import com.devlomi.commune.fragments.BaseFragment
import com.devlomi.commune.interfaces.FragmentCallback
import com.devlomi.commune.model.realms.FireCall
import com.devlomi.commune.utils.PerformCall
import com.devlomi.commune.utils.RealmHelper
import com.devlomi.commune.utils.network.FireManager
import com.devlomi.hidely.hidelyviews.HidelyImageView
import com.google.android.gms.ads.AdView
import io.realm.RealmResults
import kotlinx.android.synthetic.main.fragment_calls.*
import java.util.*

class CallsFragment : BaseFragment(), ActionMode.Callback, CallsAdapter.OnClickListener {

    override var adView: AdView? = null
    private var fireCallList: RealmResults<FireCall>? = null
    private val selectedFireCallListActionMode: MutableList<FireCall> = ArrayList()
    private lateinit var adapter: CallsAdapter
    var listener: FragmentCallback? = null
    var actionMode: ActionMode? = null
    val fireManager = FireManager()

    val viewModel: MainViewModel by activityViewModels()
    override fun showAds(): Boolean {
        return resources.getBoolean(R.bool.is_calls_ad_enabled)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as FragmentCallback
        } catch (castException: ClassCastException) {
            /** The activity does not implement the listener.  */
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_calls, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adView = view.findViewById(R.id.ad_view)
        adViewInitialized(adView)
        initAdapter()

        viewModel.queryTextChange.observe(viewLifecycleOwner, androidx.lifecycle.Observer { newText ->
            onQueryTextChange(newText)
        })
    }

    private fun initAdapter() {
        fireCallList = RealmHelper.getInstance().allCalls
        adapter = CallsAdapter(fireCallList, selectedFireCallListActionMode, activity, this@CallsFragment)
        rv_calls.layoutManager = LinearLayoutManager(activity)
        rv_calls.adapter = adapter
    }

    override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
        this.actionMode = actionMode
        actionMode.menuInflater.inflate(R.menu.menu_action_calls, menu)
        actionMode.title = "1"
        return true
    }

    override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
        return false
    }

    override fun onActionItemClicked(actionMode: ActionMode, menuItem: MenuItem): Boolean {
        if (actionMode != null && menuItem != null) {
            if (menuItem.itemId == R.id.menu_item_delete) deleteClicked()
        }
        return true
    }

    override fun onPause() {
        super.onPause()
        actionMode?.finish()
    }

    private fun deleteClicked() {
        val dialog = AlertDialog.Builder(requireActivity())
        dialog.setTitle(R.string.confirmation)
        dialog.setMessage(R.string.delete_calls_confirmation)
        dialog.setNegativeButton(R.string.no, null)
        dialog.setPositiveButton(R.string.yes) { dialogInterface, i ->
            for (fireCall in selectedFireCallListActionMode) {
                RealmHelper.getInstance().deleteCall(fireCall)
            }
            exitActionMode()
        }
        dialog.show()
    }

    override fun onDestroyActionMode(actionMode: ActionMode) {
        this.actionMode = null
        selectedFireCallListActionMode.clear()
        adapter?.notifyDataSetChanged()
    }

    private fun itemRemovedFromActionList(selectedCircle: HidelyImageView, itemView: View, fireCall: FireCall) {
        selectedFireCallListActionMode.remove(fireCall)
        if (selectedFireCallListActionMode.isEmpty()) {
            actionMode?.finish()
        } else {
            selectedCircle.hide()
            itemView.setBackgroundColor(-1)
            actionMode?.title = selectedFireCallListActionMode.size.toString() + ""
        }
    }

    private fun itemAddedToActionList(selectedCircle: HidelyImageView, itemView: View, fireCall: FireCall) {
        selectedCircle.show()
        itemView.setBackgroundColor(ContextCompat.getColor(requireContext(),R.color.item_selected_background_color))
        selectedFireCallListActionMode.add(fireCall)
        actionMode?.title = selectedFireCallListActionMode.size.toString() + ""
    }

    fun exitActionMode() {
        actionMode?.finish()
    }



    override fun onQueryTextChange(newText: String?) {
        super.onQueryTextChange(newText)

            adapter?.filter(newText)

    }

    override fun onSearchClose() {
        super.onSearchClose()
        adapter = CallsAdapter(fireCallList, selectedFireCallListActionMode, activity, this@CallsFragment)
        rv_calls.adapter = adapter
    }

    override fun onItemClick(selectedCircle: HidelyImageView, itemView: View, fireCall: FireCall) {
        if (actionMode != null) {
            if (selectedFireCallListActionMode.contains(fireCall)) itemRemovedFromActionList(selectedCircle, itemView, fireCall) else itemAddedToActionList(selectedCircle, itemView, fireCall)
        } else if (fireCall.user != null && fireCall.user.uid != null) PerformCall(requireActivity(),fireManager,disposables).performCall(fireCall.isVideo, fireCall.user.uid)
    }

    override fun onIconButtonClick(view: View, fireCall: FireCall) {
        if (actionMode != null) return
        if (fireCall.user != null && fireCall.user.uid != null) PerformCall(requireActivity(),fireManager,disposables).performCall(fireCall.isVideo, fireCall.user.uid)
    }

    override fun onLongClick(selectedCircle: HidelyImageView, itemView: View, fireCall: FireCall) {
        if (actionMode == null) {
            fragmentCallback?.startTheActionMode(this@CallsFragment)
            itemAddedToActionList(selectedCircle, itemView, fireCall)
        }
    }
}
package com.devlomi.commune.adapters;

import android.content.Context;
import androidx.annotation.Nullable;

import com.devlomi.commune.model.realms.User;

import java.util.List;

import io.realm.OrderedRealmCollection;

public class NewGroupAdapter extends ForwardAdapter {


    public NewGroupAdapter(@Nullable OrderedRealmCollection<User> data, List<User> selectedForwardedUsers, List<User> currentGroupUsers, boolean autoUpdate,
                           Context context, OnUserClick onUserClick) {
        super(data, selectedForwardedUsers, currentGroupUsers, autoUpdate, context, onUserClick);
    }
    public NewGroupAdapter(@Nullable OrderedRealmCollection<User> data, List<User> selectedForwardedUsers, List<User> currentGroupUsers, boolean isBroadcast, boolean autoUpdate,
                           Context context, OnUserClick onUserClick) {
        super(data, selectedForwardedUsers, currentGroupUsers,isBroadcast, autoUpdate, context, onUserClick);
    }
}

package com.devlomi.commune.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.devlomi.commune.utils.IntentUtils;
import com.devlomi.commune.utils.NotificationHelper;
import com.devlomi.commune.utils.RealmHelper;
import com.devlomi.commune.utils.network.FireManager;


//this will handle the reply from notification on API24+
public class MarkAsReadReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (action.equals(IntentUtils.INTENT_ACTION_MARK_AS_READ)) {
                //get user to send the message
                String chatId = intent.getStringExtra(IntentUtils.EXTRA_CHAT_ID);

                //get the replied text
                boolean isGroup = intent.getBooleanExtra(IntentUtils.IS_GROUP, false);
                new NotificationHelper(context).dismissNotification(chatId, true);

                updateIncomingMessagesState(context, isGroup, chatId);

            }
        }
    }

    //set incoming messages to read
    private void updateIncomingMessagesState(Context context, boolean isGroup, String chatId) {
        if (isGroup) {
            //set received messages as read
            RealmHelper.getInstance().setMessagesAsReadLocally(chatId);
        } else {
            //update received messages as read in Server
            new FireManager().setMessagesAsRead(context, chatId);
        }
    }

}

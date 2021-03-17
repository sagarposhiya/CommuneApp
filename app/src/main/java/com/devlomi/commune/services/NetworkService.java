package com.devlomi.commune.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.devlomi.commune.model.realms.GroupEvent;
import com.devlomi.commune.model.realms.Message;
import com.devlomi.commune.utils.DownloadManager;
import com.devlomi.commune.utils.IntentUtils;
import com.devlomi.commune.utils.RealmHelper;
import com.devlomi.commune.utils.network.CallsManager;
import com.devlomi.commune.utils.network.FireManager;
import com.devlomi.commune.utils.network.GroupManager;

import io.reactivex.disposables.CompositeDisposable;


/**
 * Created by Devlomi on 31/12/2017.
 */

//this is responsible for sending and receiving files/data from firebase using Download Manager Class
public class NetworkService extends Service {
    private CompositeDisposable disposables = new CompositeDisposable();
    private FireManager fireManager = new FireManager();
    private GroupManager groupManager = new GroupManager();
    private CallsManager callsManager = new CallsManager();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String chatId = intent.getStringExtra(IntentUtils.EXTRA_CHAT_ID);
            if (intent.getAction().equals(IntentUtils.INTENT_ACTION_UPDATE_GROUP)) {

                GroupEvent groupEvent = intent.getParcelableExtra(IntentUtils.EXTRA_GROUP_EVENT);
                String groupId = intent.getStringExtra(IntentUtils.EXTRA_GROUP_ID);


                disposables.add(groupManager.updateGroup(groupId, groupEvent).subscribe());
            }
            if (intent.getAction().equals(IntentUtils.INTENT_ACTION_FETCH_AND_CREATE_GROUP)) {
                String groupId = intent.getStringExtra(IntentUtils.EXTRA_GROUP_ID);
                disposables.add(groupManager.fetchAndCreateGroup(groupId).subscribe());

            } else if (intent.getAction().equals(IntentUtils.INTENT_ACTION_HANDLE_REPLY)) {
                String messageId = intent.getStringExtra(IntentUtils.EXTRA_MESSAGE_ID);
                final Message message = RealmHelper.getInstance().getMessage(messageId, chatId);
                if (message != null) {
                    DownloadManager.sendMessage(message, new DownloadManager.OnComplete() {
                        @Override
                        public void onComplete(boolean isSuccessful) {
                            if (isSuccessful) {
                                //set other unread messages as read
                                if (!message.isGroup())
                                    fireManager.setMessagesAsRead(NetworkService.this, message.getChatId());
                                //update unread count to 0
                            }
                        }
                    });
                }
            } else if (intent.getAction().equals(IntentUtils.INTENT_ACTION_SET_CALL_ENDED)) {
                String callId = intent.getStringExtra(IntentUtils.CALL_ID);
                String otherUid = intent.getStringExtra(IntentUtils.OTHER_UID);
                boolean isIncoming = intent.getBooleanExtra(IntentUtils.IS_INCOMING, false);
                disposables.add(
                        callsManager.setCallEnded(callId, otherUid, isIncoming).subscribe(() -> {
                }, throwable -> { })

                );
            } else if (intent.getAction().equals(IntentUtils.INTENT_ACTION_SET_CALL_DECLINED_FOR_GROUP)){
                String callId = intent.getStringExtra(IntentUtils.CALL_ID);
                String groupId = intent.getStringExtra(IntentUtils.EXTRA_GROUP_ID);

                disposables.add(
                        callsManager.setCallRejectedForGroup(callId, groupId).subscribe(() -> { } , throwable -> { } )
                );

            } else {
                String messageId = intent.getStringExtra(IntentUtils.EXTRA_MESSAGE_ID);
                if (intent.getAction().equals(IntentUtils.INTENT_ACTION_UPDATE_MESSAGE_STATE)) {
                    String myUid = intent.getStringExtra(IntentUtils.EXTRA_MY_UID);
                    int state = intent.getIntExtra(IntentUtils.EXTRA_STAT, 0);
                    updateMessageStat(messageId, myUid, chatId, state);
                } else if (intent.getAction().equals(IntentUtils.INTENT_ACTION_UPDATE_VOICE_MESSAGE_STATE)) {
                    String myUid = intent.getStringExtra(IntentUtils.EXTRA_MY_UID);
                    updateVoiceMessageStat(messageId, chatId, myUid);
                } else {
                    Message message = RealmHelper.getInstance().getMessage(messageId, chatId);
                    if (message != null) {
                        DownloadManager.request(message, null);
                    }
                }
            }
        }
        return START_STICKY;
    }


    public void updateMessageStat(final String messageId, final String myUid, final String chatId, final int state) {
        disposables.add(fireManager.updateMessagesState(myUid, messageId, state, false).subscribe());

    }


    public void updateVoiceMessageStat(final String messageId, final String chatId, final String myUid) {
        disposables.add(fireManager.updateVoiceMessageStat(myUid, messageId).subscribe());
    }


    @Override
    public void onDestroy() {
        DownloadManager.cancelAllTasks();
        super.onDestroy();
        startService(new Intent(this, NetworkService.class));
        disposables.dispose();

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}

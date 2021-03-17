package com.devlomi.commune.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import com.devlomi.commune.R;
import com.devlomi.commune.activities.calling.CallingActivity;
import com.devlomi.commune.activities.main.MainActivity;
import com.devlomi.commune.activities.main.messaging.ChatActivity;
import com.devlomi.commune.model.realms.Chat;
import com.devlomi.commune.model.realms.FireCall;
import com.devlomi.commune.model.realms.Message;
import com.devlomi.commune.model.realms.User;
import com.devlomi.commune.receivers.HandleReplyReceiver;
import com.devlomi.commune.receivers.MarkAsReadReceiver;
import com.devlomi.commune.services.CallingService;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import io.realm.RealmList;
import me.leolin.shortcutbadger.ShortcutBadger;

import static com.devlomi.commune.utils.IntentUtils.NOTIFICATION_ACTION_DECLINE;
import static com.devlomi.commune.utils.MessageTypeHelper.getMessageContent;

public class NotificationHelper extends ContextWrapper {

    private static final String KEY_NOTIFICATION_GROUP = "handleNewMessage-group";
    public static final String LABEL_REPLY = "Reply";
    public static final String KEY_PRESSED_ACTION = "KEY_PRESSED_ACTION";
    public static final String KEY_TEXT_REPLY = "KEY_TEXT_REPLY";

    //this is used to handleNewMessage on devices below API24 since it will be only one notification
    public static final int ID_NOTIFICATION = 1;
    public static final int ID_NOTIFICATION_AUDIO = -2;
    public static final int ID_GROUP_NOTIFICATION = -1;

    public static final String NOTIFICATION_CHANNEL_NAME_MESSAGES = "Messages Notifications";
    public static final String NOTIFICATION_CHANNEL_ID_MESSAGES = "Messages_Notifications_ID";
    public static final String NOTIFICATION_CHANNEL_NAME_AUDIO = "Audio Notifications";
    public static final String NOTIFICATION_CHANNEL_ID_AUDIO = "Audio_Notifications_ID";
    public static final String NOTIFICATION_CHANNEL_ID_CALLING = "Calling-Notifications_ID";
    public static final String NOTIFICATION_CHANNEL_ID_INCOMING_CALLS = "Incoming-Calls-Notifications_ID";
    public static final String NOTIFICATION_CHANNEL_NAME_CALLING = "Calls Notifications";
    public static final String NOTIFICATION_CHANNEL_NAME_INCOMING_CALLS = "Incoming Calls Notifications";


    public static final int PI_REQUEST_CODE_DECLINE = 3;
    public static final int PI_REQUEST_CODE_ANSWER = 4;
    public static final int PI_REQUEST_CODE_CLICK = 5;

    private static int incomingCallNotificationId = -1;

    private NotificationManager manager;


    public NotificationHelper(Context base) {
        super(base);
        //create notification channels for android Oreo+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            NotificationChannel messagesChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_MESSAGES,
                    NOTIFICATION_CHANNEL_NAME_MESSAGES, NotificationManager.IMPORTANCE_HIGH);

            messagesChannel.setVibrationPattern(getVibrationPattern());
            getManager().createNotificationChannel(messagesChannel);

            NotificationChannel audioChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_AUDIO,
                    NOTIFICATION_CHANNEL_NAME_AUDIO, NotificationManager.IMPORTANCE_DEFAULT);
            audioChannel.setSound(null, null);

            getManager().createNotificationChannel(audioChannel);


            NotificationChannel callsChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_CALLING,
                    NOTIFICATION_CHANNEL_NAME_CALLING, NotificationManager.IMPORTANCE_DEFAULT);

            callsChannel.setSound(null, null);

            getManager().createNotificationChannel(callsChannel);


            NotificationChannel incomingCallsChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_INCOMING_CALLS,
                    NOTIFICATION_CHANNEL_NAME_INCOMING_CALLS, NotificationManager.IMPORTANCE_HIGH);

            incomingCallsChannel.setSound(null, null);

            getManager().createNotificationChannel(incomingCallsChannel);

        }
    }

    private long[] getVibrationPattern() {
        return SharedPreferencesManager.isVibrateEnabled() ? new long[]{200, 200} : new long[0];
    }


    //get summary text (30 messages from 2 chats for example)
    private String getSubText(int messagesCount, int chatsCount) {
        String chats = chatsCount == 1 ? " Chat" : " Chats";
        String messages = messagesCount == 1 ? " Message" : " Messages";
        if (chatsCount <= 1) {
            return messagesCount + " New " + messages;
        }
        return messagesCount + messages + " from " + chatsCount + chats;
    }

    private String getUserNameWithNumOfMessages(int unreadCount,
                                                String userName) {
        if (unreadCount == 0 || unreadCount == 1)
            return userName;

        return userName + " " + "(" + unreadCount + " Messages" + ")" + " ";

    }

    public static boolean isBelowApi24() {
        return Build.VERSION.SDK_INT < 24;
    }


    private Bitmap getProfilePhotoAsBitmap(String thumbImg) {
        if (thumbImg != null)
            return BitmapUtils.encodeImage(thumbImg);

        //if thumbImg is not exists like the user data is not downloaded yet
        //then get the default placeholder image
        return BitmapUtils.getBitmapFromVectorDrawable(this, R.drawable.user_img);

    }

    private String getSenderName(String userName, String groupName) {

        if (isBelowApi24() && groupName != null) {
            return userName + " @ " + groupName;
        }


        return userName;
    }

    //dismiss notification when open the chat activity with certain user
    public void dismissNotification(String chatId, boolean decrementCount) {

        Chat chat = RealmHelper.getInstance().getChat(chatId);
        if (chat != null) {

            int notificationId = isBelowApi24() ? ID_NOTIFICATION : chat.getNotificationId();
            getManager().cancel(notificationId);
            if (decrementCount) {
                updateNotificationCount(0);
                RealmHelper.getInstance().deleteUnReadMessages(chatId);
            }
            //dismiss grouped notification if there are no notifications left
            if (!isBelowApi24() && !RealmHelper.getInstance().areThereUnreadChats()) {
                //dismiss grouped notifications
                getManager().cancel(ID_GROUP_NOTIFICATION);
            }
        }
    }

    //update Notification Count badge (in launcher)
    public void updateNotificationCount(int messagesCount) {

        if (messagesCount >= 0) {
            ShortcutBadger.applyCount(this, messagesCount);
        }
    }


    public void fireNotification(String newMessageChatId) {
        boolean isNotificationsEnabled = SharedPreferencesManager.isNotificationEnabled();
        long unreadMessagesCount = RealmHelper.getInstance().getUnreadMessagesCount();

        //below api24 (no notifications grouping feature)
        if (isBelowApi24()) {
            //store chats in hash map as it may occurs more than once rather than getting the same chat every time, it's kinda 'caching' logic
            HashMap<String, Chat> chatHashMap = new HashMap<>();
            //get last7Unread messages because it's the maximum number for messages to show in a notification
            List<Message> last7UnreadMessages = RealmHelper.getInstance().getLast7UnreadMessages();
            final NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle("");
            //get all unread chats count
            int chatsCount = (int) RealmHelper.getInstance().getUnreadChatsCount();
            String lastMessageTimestamp;

            Chat lastChat;

            //if message was deleted dismiss notification
            if (last7UnreadMessages.isEmpty()) {
                getManager().cancel(ID_NOTIFICATION);
            } else {

                for (int i = 0; i < last7UnreadMessages.size(); i++) {

                    Message unreadMessage = last7UnreadMessages.get(i);

                    String chatId = unreadMessage.getChatId();
                    Chat chat;
                    //if chat is exists in hashmap get it
                    if (chatHashMap.containsKey(chatId)) {
                        chat = chatHashMap.get(chatId);
                        //otherwise get it from realm and save it to hashmap
                    } else {
                        chat = RealmHelper.getInstance().getChat(chatId);
                        chatHashMap.put(chatId, chat);
                    }

                    lastChat = chat;

                    lastMessageTimestamp = unreadMessage.getTimestamp();

                    //if chat has not enable notifications do nothing
                    //otherwise fill the notification
                    if (isNotificationsEnabled && !chat.isMuted()) {
                        String sender = "";
                        if (chat.getUser().isGroupBool()) {
                            RealmList<User> users = chat.getUser().getGroup().getUsers();
                            User user = ListUtil.getUserById(unreadMessage.getFromId(), users);
                            if (user != null) {
                                sender = getSenderName(user.getProperUserName(), chat.getUser().getProperUserName());
                            }
                        } else {
                            if (chatsCount > 1)
                                sender = getSenderName(chat.getUser().getProperUserName(), null);
                        }

                        messagingStyle.addMessage(getMessageContent(unreadMessage, true), Long.parseLong(unreadMessage.getTimestamp()), sender);

                    }


                    NotificationCompat.Builder notificationBuilder =
                            createNotificationBuilder(
                                    ""
                                    , ""
                                    , chat, chatsCount);


                    if (chatHashMap.size() > 1) {
                        String appName = this.getResources().getString(R.string.app_name);
                        messagingStyle.setConversationTitle(appName);
                        notificationBuilder.setContentIntent(getPendingIntent(null));

                    } else {
                        messagingStyle.setConversationTitle(lastChat.getUser().getProperUserName());
                        notificationBuilder.setContentIntent(getPendingIntent(lastChat));
                    }


                    notificationBuilder.setSubText(getSubText((int) unreadMessagesCount, chatsCount));


                    notificationBuilder.setStyle(messagingStyle);

                    //set the timestamp of the last message
                    if (!lastMessageTimestamp.equals(""))
                        notificationBuilder.setWhen(Long.parseLong(lastMessageTimestamp));


                    //notify once after filling the messages
                    if (i == last7UnreadMessages.size() - 1)
                        getManager().notify(ID_NOTIFICATION, notificationBuilder.build());
                }
            }

            updateNotificationCount((int) unreadMessagesCount);

            //api24+ (grouping feature)
        } else {

            List<Chat> unreadChats = RealmHelper.getInstance().getUnreadChats();

            for (Chat unreadChat : unreadChats) {

                final NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
                final NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle("");

                if (isNotificationsEnabled && !unreadChat.isMuted()) {


                    String userNameWithNumOfMessages = getUserNameWithNumOfMessages(unreadChat.getUnReadCount(), unreadChat.getUser().getProperUserName());

                    messagingStyle.setConversationTitle(userNameWithNumOfMessages);


                    //if it's a group or it's below api24 we will use messaging style
                    boolean isGroup = unreadChat.getUser().isGroupBool();

                    List<Message> messageList = RealmHelper.getInstance().filterUnreadMessages(unreadChat.getUnreadMessages());
                    if (isGroup) {
                        RealmList<User> users = unreadChat.getUser().getGroup().getUsers();

                        for (Message unreadMessage : messageList) {
                            User user = ListUtil.getUserById(unreadMessage.getFromId(), users);
                            if (user != null) {
                                String sender = getSenderName(user.getProperUserName(), unreadChat.getUser().getProperUserName());
                                messagingStyle.addMessage(getMessageContent(unreadMessage, true), Long.parseLong(unreadMessage.getTimestamp()), sender);
                            }
                        }
                    } else {
                        for (Message unreadMessage : messageList) {
                            inboxStyle.addLine(getMessageContent(unreadMessage, true));
                        }
                    }


                    NotificationCompat.Builder notificationBuilder =
                            createNotificationBuilder(
                                    userNameWithNumOfMessages
                                    , getMessageContent(unreadChat.getUnreadMessages().last(), true)
                                    , unreadChat, unreadChats.size());

                    //if it's a group we will use messaging style,otherwise we will user inboxStyle
                    notificationBuilder.setStyle(isGroup || isBelowApi24() ? messagingStyle : inboxStyle);

                    if (!unreadChat.getLastMessageTimestamp().equals(""))
                        notificationBuilder.setWhen(Long.parseLong(unreadChat.getLastMessageTimestamp()));


                    NotificationCompat.Builder groupNotification;

                    PendingIntent pendingIntent = getPendingIntent(unreadChat);
                    notificationBuilder.setContentIntent(pendingIntent);

                    //if it's a new message fire the sound and vibration if enabled,otherwise just show other notifications without bombarding the user
                    notificationBuilder.setOnlyAlertOnce(newMessageChatId.equals(unreadChat.getChatId()) ? false : true);


                    int notificationId = unreadChat.getNotificationId();
                    //group multiple notification in one grouped notification
                    groupNotification = createNotificationBuilder("", "", null, unreadChats.size());

                    if (!unreadChat.getLastMessageTimestamp().equals(""))
                        groupNotification.setWhen(Long.parseLong(unreadChat.getLastMessageTimestamp()));


                    //group notification by user id
                    groupNotification.setGroupSummary(true).setGroup(KEY_NOTIFICATION_GROUP);

                    //if it's a new message fire the sound and vibration if enabled,other wise just show other notifications without bombarding the user
                    groupNotification.setOnlyAlertOnce(true);

                    groupNotification.setContentTitle("");

                    //set Summary (eg. 10 Messages from 3 Chat)
                    String subText = getSubText((int) unreadMessagesCount, unreadChats.size());

                    groupNotification.setSubText(subText);

                    notificationBuilder.setGroup(KEY_NOTIFICATION_GROUP);


                    notificationBuilder.addAction(getReplyActionInput(unreadChat));
                    notificationBuilder.addAction(getMarkAsReadAction(unreadChat));


                    getManager().notify(notificationId, notificationBuilder.build());
                    getManager().notify(ID_GROUP_NOTIFICATION, groupNotification.build());

                }
            }
            updateNotificationCount((int) unreadMessagesCount);
        }

    }


    //get onClick intent
    private PendingIntent getPendingIntent(Chat chat) {
        PendingIntent pendingIntent;
        if (isBelowApi24()) {
            //if it's only from one user then open the chatActivity with this user
            if (chat != null) {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra(IntentUtils.UID, chat.getChatId());
                //adding stack for user (to prevent kill the app when click back since there is no previous activity launched
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                stackBuilder.addNextIntentWithParentStack(intent);
                pendingIntent = stackBuilder.getPendingIntent(ID_NOTIFICATION, PendingIntent.FLAG_UPDATE_CURRENT);
            } else {
                //otherwise there are multiple messages from multiple users therefore just open Main Activity
                Intent intent = new Intent(this, MainActivity.class);
                pendingIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            }

        } else {
//        start chatActivity with the clicked user
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra(IntentUtils.UID, chat.getChatId());
            //adding stack for user (to prevent kill the app when click back since there is no previous activity launched
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addNextIntentWithParentStack(intent);
            pendingIntent = stackBuilder.getPendingIntent(chat.getNotificationId(), PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return pendingIntent;
    }

    private NotificationCompat.Builder createNotificationBuilder(
            String title, String message, Chat chat, int chatsCount) {


        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID_MESSAGES)
                //set app icon
                .setSmallIcon(R.drawable.ic_noti_icon)
                .setContentTitle(title)
                .setContentText(message)
                //color
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                //Notification Sound (get it from shared preferences)
                .setSound(SharedPreferencesManager.getRingtone())
                //high priority to make it show as heads-up
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                //set vibrate if it's enabled by the user
                .setVibrate(getVibrationPattern());

        if (chat != null) {
            User user = chat.getUser();
            Bitmap largeIcon = getProfilePhotoAsBitmap(user.getThumbImg());

            if (!isBelowApi24() || chatsCount == 1)
                builder.setLargeIcon(largeIcon);

        } else {
            Bitmap largeIcon = getProfilePhotoAsBitmap(null);
            builder.setLargeIcon(largeIcon);
        }


        return builder;
    }


    //set reply from notification action
    private NotificationCompat.Action getReplyActionInput(Chat chat) {
        RemoteInput remoteInput = new RemoteInput.Builder(KEY_TEXT_REPLY)
                .setLabel(this.getString(R.string.reply))
                .build();

        PendingIntent replyIntent = PendingIntent.getBroadcast(this
                , chat.getNotificationId()
                , getMessageReplyIntent(LABEL_REPLY, chat.getChatId())
                , PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Action replyAction =
                new NotificationCompat.Action.Builder(android.R.drawable.sym_def_app_icon,
                        this.getString(R.string.reply), replyIntent)
                        .addRemoteInput(remoteInput)
                        .build();

        return replyAction;
    }

    //set reply from notification action
    private NotificationCompat.Action getMarkAsReadAction(Chat chat) {

        PendingIntent markAsReadIntent = PendingIntent.getBroadcast(this
                , chat.getNotificationId()
                , getMarkAsReadIntent(chat.getChatId(), chat.getUser().isGroupBool())
                , PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Action markAsReadAction =
                new NotificationCompat.Action.Builder(android.R.drawable.sym_def_app_icon,
                        this.getString(R.string.mark_as_read), markAsReadIntent)
                        .build();

        return markAsReadAction;
    }


    //start service when click on Reply and pass the user
    private Intent getMessageReplyIntent(String label, String chatId) {
        Intent intent = new Intent(this, HandleReplyReceiver.class);
        intent
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .setAction(IntentUtils.INTENT_ACTION_HANDLE_REPLY)
                .putExtra(KEY_PRESSED_ACTION, label)
                .putExtra(IntentUtils.UID, chatId)
                .putExtra(IntentUtils.EXTRA_CHAT_ID, chatId);
        return intent;
    }

    private Intent getMarkAsReadIntent(String chatId, boolean isGroup) {
        Intent intent = new Intent(this, MarkAsReadReceiver.class);
        intent
                .setAction(IntentUtils.INTENT_ACTION_MARK_AS_READ)
                .putExtra(IntentUtils.EXTRA_CHAT_ID, chatId)
                .putExtra(IntentUtils.IS_GROUP, isGroup);
        return intent;
    }

    public void messageDeleted(Message message) {
        if (message != null) {
            String chatId = message.getChatId();
            if (!MyApp.getCurrentChatId().equals(chatId)) {
                Chat chat = RealmHelper.getInstance().getChat(chatId);
                if (chat != null) {
                    //if it's below api24 update notifications
                    if (isBelowApi24()) {
                        fireNotification(null);
                    } else {
                        //if it's higher than API 24
                        // check if the chat has unread messages
                        //if so update notifications
                        if (chat.getUnReadCount() > 0) {
                            fireNotification(message.getChatId());
                        } else {
                            //otherwise dismiss notification
                            dismissNotification(chatId, false);
                        }
                    }
                }
            }
        }
    }

    private NotificationManager getManager() {
        if (manager == null) {
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return manager;
    }

    public Notification getAudioNotification() {
        return new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID_AUDIO)
                //set app icon
                .setSmallIcon(R.drawable.ic_noti_icon)
                .setContentTitle(getResources().getString(R.string.playing_audio))
                .setContentText(getResources().getString(R.string.playing_audio))
                //color
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();
    }

    public Notification getCallsNotifications(String userName) {
        return new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID_AUDIO)
                //set app icon
                .setSmallIcon(R.drawable.ic_noti_icon)
                .setContentTitle(userName)
                .setContentText(userName)
                //color
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }


    public void createMissedCallNotification(User user, String phone) {


        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_CALLING)
                        .setSmallIcon(R.drawable.ic_noti_icon)
                        .setContentTitle(getString(R.string.missed_call_notification))
                        .setContentText(user == null ? phone : user.getProperUserName());

        if (user != null) {
            Bitmap largeIcon = getProfilePhotoAsBitmap(user.getThumbImg());
            mBuilder.setLargeIcon(largeIcon);
        }

        mBuilder.setContentIntent(contentIntent);
        mBuilder.setDefaults(Notification.DEFAULT_SOUND);
        mBuilder.setAutoCancel(true);

        if (incomingCallNotificationId != -1)
            getManager().cancel(incomingCallNotificationId);

        getManager().notify(generateId(), mBuilder.build());
    }

    public Notification createActiveCallNotification(FireCall fireCall, int notificationId) {

        User user = fireCall.getUser();
        String phoneNumber = fireCall.getPhoneNumber();
        PendingIntent notificationPIntent = getNotificationClickPendingIntent(fireCall, IntentUtils.NOTIFICATION_ACTION_CLICK, PI_REQUEST_CODE_CLICK);

        String title = fireCall.isVideo() ? getString(R.string.video_call) : getString(R.string.voice_call);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_CALLING)
                        .setSmallIcon(R.drawable.ic_noti_icon)
                        .setContentTitle(title)
                        .setOnlyAlertOnce(true)
                        .setContentIntent(notificationPIntent)
                        .setContentText(user == null ? phoneNumber : user.getProperUserName());


        if (user != null) {
            Bitmap largeIcon = getProfilePhotoAsBitmap(user.getThumbImg());
            mBuilder.setLargeIcon(largeIcon);
        }


        Intent hangupIntent = getCallingActivityIntent(fireCall, IntentUtils.NOTIFICATION_ACTION_HANGUP);
        PendingIntent hangupPIntent = PendingIntent.getActivity(this, notificationId, hangupIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action hangupAction = new NotificationCompat.Action(R.drawable.baseline_call_end_black_24, getString(R.string.hangup), hangupPIntent);


        mBuilder.addAction(hangupAction);
        mBuilder.setAutoCancel(true);
        return mBuilder.build();
    }

    private PendingIntent getNotificationClickPendingIntent(FireCall fireCall, int action, int requestCode) {

        Intent notificationIntent = getCallingActivityIntent(fireCall, action);

        return PendingIntent.getActivity(this, requestCode, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    private Intent getCallingActivityIntent(FireCall fireCall, int action) {
        Intent notificationIntent = new Intent(this, CallingActivity.class);
        String callId = fireCall.getCallId();
        notificationIntent.putExtra(IntentUtils.CALL_DIRECTION, fireCall.getDirection());
        notificationIntent.putExtra(IntentUtils.CALL_TYPE, fireCall.getCallType());
        notificationIntent.putExtra(IntentUtils.CALL_ID, callId);
        notificationIntent.putExtra(IntentUtils.UID, fireCall.getUser().getUid());
        notificationIntent.putExtra(IntentUtils.PHONE, fireCall.getPhoneNumber());
        notificationIntent.putExtra(IntentUtils.CALL_ACTION_TYPE, action);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        return notificationIntent;
    }

    public Notification createIncomingCallNotification(FireCall fireCall, int notificationId) {
        PendingIntent notificationPIntent = getNotificationClickPendingIntent(fireCall, IntentUtils.NOTIFICATION_ACTION_START_INCOMING, PI_REQUEST_CODE_CLICK);
        String title = fireCall.isVideo() ? getString(R.string.incoming_video_call) : getString(R.string.incoming_voice_call);

        User user = fireCall.getUser();
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_INCOMING_CALLS)
                        .setSmallIcon(R.drawable.ic_noti_icon)
                        .setContentTitle(title)
                        .setFullScreenIntent(notificationPIntent, true)
                        .setContentText(user == null ? fireCall.getPhoneNumber() : user.getProperUserName());

        if (user != null) {
            Bitmap largeIcon = getProfilePhotoAsBitmap(user.getThumbImg());
            mBuilder.setLargeIcon(largeIcon);
        }


//        Intent answerIntent = new Intent(this, CallingServiceAgora.class);
//        answerIntent.putExtra(IntentUtils.CALL_ID, callId);
//        answerIntent.putExtra(IntentUtils.CALL_ACTION_TYPE, IntentUtils.NOTIFICATION_ACTION_ANSWER);
//        PendingIntent pendingIntent = PendingIntent.getService(this, PI_REQUEST_CODE_ANSWER, answerIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent answerPIntent = getNotificationClickPendingIntent(fireCall, IntentUtils.NOTIFICATION_ACTION_ANSWER, PI_REQUEST_CODE_ANSWER);

        NotificationCompat.Action answerAction = new NotificationCompat.Action(R.drawable.baseline_phone_black_24, getString(R.string.answer), answerPIntent);


        Intent declineIntent = CallingService.Companion.getStartIntent(this, fireCall, NOTIFICATION_ACTION_DECLINE);
        PendingIntent declinePIntent = PendingIntent.getService(this, PI_REQUEST_CODE_DECLINE, declineIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action declineAction = new NotificationCompat.Action(R.drawable.baseline_call_end_black_24, getString(R.string.decline), declinePIntent);


        mBuilder.addAction(answerAction);
        mBuilder.addAction(declineAction);
        mBuilder.setDefaults(Notification.DEFAULT_SOUND);
        mBuilder.setAutoCancel(true);

        incomingCallNotificationId = notificationId;
        return mBuilder.build();
    }

    public void cancelIncomingCallNotification() {
        getManager().cancel(incomingCallNotificationId);
    }

    public static int generateId() {
        return (int) ((new Date().getTime() / 1000L) % Integer.MAX_VALUE);
    }

    public void notifyNotification(int notificationId, Notification notification) {
        getManager().notify(notificationId, notification);
    }
}


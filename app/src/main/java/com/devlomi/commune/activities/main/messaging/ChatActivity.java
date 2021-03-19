package com.devlomi.commune.activities.main.messaging;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.app.SharedElementCallback;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cjt2325.cameralibrary.ResultCodes;
import com.codekidlabs.storagechooser.StorageChooser;
import com.devlomi.commune.R;
import com.devlomi.commune.activities.BaseActivity;
import com.devlomi.commune.activities.CameraActivity;
import com.devlomi.commune.activities.ContactDetailsActivity;
import com.devlomi.commune.activities.ForwardActivity;
import com.devlomi.commune.activities.FullscreenActivity;
import com.devlomi.commune.activities.SelectContactNumbersActivity;
import com.devlomi.commune.activities.UserDetailsActivity;
import com.devlomi.commune.activities.ViewStatusActivity;
import com.devlomi.commune.activities.main.messaging.swipe.MessageSwipeController;
import com.devlomi.commune.activities.main.messaging.swipe.SwipeControllerActions;
import com.devlomi.commune.adapters.messaging.AudibleInteraction;
import com.devlomi.commune.adapters.messaging.ContactHolderInteraction;
import com.devlomi.commune.adapters.messaging.Interaction;
import com.devlomi.commune.adapters.messaging.MessagingAdapter;
import com.devlomi.commune.events.AudioServiceCallbacksEvent;
import com.devlomi.commune.events.GroupActiveStateChanged;
import com.devlomi.commune.events.HeadsetStateChanged;
import com.devlomi.commune.events.OnNetworkComplete;
import com.devlomi.commune.events.UpdateGroupEvent;
import com.devlomi.commune.events.UpdateNetworkProgress;
import com.devlomi.commune.model.ExpandableContact;
import com.devlomi.commune.model.ProgressData;
import com.devlomi.commune.model.constants.DownloadUploadStat;
import com.devlomi.commune.model.constants.MessageStat;
import com.devlomi.commune.model.constants.MessageType;
import com.devlomi.commune.model.constants.TypingStat;
import com.devlomi.commune.model.realms.Chat;
import com.devlomi.commune.model.realms.Group;
import com.devlomi.commune.model.realms.Message;
import com.devlomi.commune.model.realms.PhoneNumber;
import com.devlomi.commune.model.realms.QuotedMessage;
import com.devlomi.commune.model.realms.RealmContact;
import com.devlomi.commune.model.realms.Status;
import com.devlomi.commune.model.realms.User;
import com.devlomi.commune.placespicker.Place;
import com.devlomi.commune.placespicker.PlacesPickerActivity;
import com.devlomi.commune.receivers.HeadsetReceiver;
import com.devlomi.commune.services.AudioService;
import com.devlomi.commune.utils.AdapterHelper;
import com.devlomi.commune.utils.BitmapUtils;
import com.devlomi.commune.utils.ClipboardUtil;
import com.devlomi.commune.utils.ContactUtils;
import com.devlomi.commune.utils.DirManager;
import com.devlomi.commune.utils.DownloadManager;
import com.devlomi.commune.utils.DpUtil;
import com.devlomi.commune.utils.FileFilter;
import com.devlomi.commune.utils.FileUtils;
import com.devlomi.commune.utils.FireConstants;
import com.devlomi.commune.utils.FireListener;
import com.devlomi.commune.utils.GroupTyping;
import com.devlomi.commune.utils.IntentUtils;
import com.devlomi.commune.utils.KeyboardHelper;
import com.devlomi.commune.utils.MessageCreator;
import com.devlomi.commune.utils.MessageTypeHelper;
import com.devlomi.commune.utils.MimeTypes;
import com.devlomi.commune.utils.MyApp;
import com.devlomi.commune.utils.NetworkHelper;
import com.devlomi.commune.utils.NotificationHelper;
import com.devlomi.commune.utils.PerformCall;
import com.devlomi.commune.utils.RealPathUtil;
import com.devlomi.commune.utils.RealmHelper;
import com.devlomi.commune.utils.RecorderSettings;
import com.devlomi.commune.utils.ServiceHelper;
import com.devlomi.commune.utils.SharedPreferencesManager;
import com.devlomi.commune.utils.SnackbarUtil;
import com.devlomi.commune.utils.StringUtils;
import com.devlomi.commune.utils.TimeHelper;
import com.devlomi.commune.utils.Util;
import com.devlomi.commune.utils.network.FireManager;
import com.devlomi.commune.utils.network.GroupManager;
import com.devlomi.commune.views.AnimButton;
import com.devlomi.commune.views.AttachmentView;
import com.devlomi.commune.views.ChatEditText;
import com.devlomi.commune.views.dialogs.DeleteDialog;
import com.devlomi.commune.views.dialogs.DialogChoseNumber;
import com.devlomi.hidely.hidelyviews.HidelyImageButton;
import com.devlomi.record_view.OnBasketAnimationEnd;
import com.devlomi.record_view.OnRecordClickListener;
import com.devlomi.record_view.OnRecordListener;
import com.devlomi.record_view.RecordView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.vanniktech.emoji.EmojiPopup;
import com.vanniktech.emoji.EmojiTextView;
import com.wafflecopter.multicontactpicker.ContactResult;
import com.wafflecopter.multicontactpicker.MultiContactPicker;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import ca.barrenechea.widget.recyclerview.decoration.StickyHeaderDecoration;
import de.hdodenhof.circleimageview.CircleImageView;
import io.reactivex.disposables.Disposable;
import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.RealmList;
import io.realm.RealmResults;
import omrecorder.AudioChunk;
import omrecorder.OmRecorder;
import omrecorder.PullTransport;
import omrecorder.Recorder;

import static com.devlomi.commune.model.constants.DownloadUploadStat.CANCELLED;
import static com.devlomi.commune.model.constants.DownloadUploadStat.FAILED;
import static com.devlomi.commune.model.constants.DownloadUploadStat.LOADING;
import static com.devlomi.commune.utils.FireConstants.presenceRef;
import static com.devlomi.commune.utils.IntentUtils.EXTRA_CURRENT_ALBUM_POSITION;
import static com.devlomi.commune.utils.IntentUtils.EXTRA_CURRENT_MESSAGE_ID;
import static com.devlomi.commune.utils.IntentUtils.EXTRA_STARTING_POSITION;


public class ChatActivity extends BaseActivity implements GroupTyping.GroupTypingListener, Interaction, ContactHolderInteraction, AudibleInteraction {

    //random numbers just to identify requestCode
    public static final int PICK_MUSIC_REQUEST = 159;
    public static final int CAMERA_REQUEST = 4659;
    public static final int PICK_GALLERY_REQUEST = 4815;
    public static final int FORWARD_MESSAGE_REQUEST = 4981;
    public static final int PICK_CONTACT_REQUEST = 5491;
    public static final int PICK_NUMBERS_FOR_CONTACT_REQUEST = 5517;
    public static final int PICK_LOCATION_REQUEST = 7125;

    public static int MAX_FILE_SIZE = 40000;
    public static final int MAX_SELECTABLE = 9;


    //update last seen every 120000 Seconds (2 Minutes)
    private static final int UPDATE_PRESENCE_DELAY = 120000;
    Handler updatePresenceHandler = new Handler();

    //start voice record after 575ms until the sound effect finishes
    public static final int RECORD_START_AUDIO_LENGTH = 575;

    //value to indicate whether it's in action mode or not
    public boolean isInActionMode = false;
    //value to indicate whether it's in search mode or not
    private boolean isInSearchMode = false;

    private RecyclerView recyclerView;
    private ImageView imgAttachment, cameraBtn, emojiBtn;
    private ImageButton btnToolbarBack, upArrowSearchToolbar, downArrowSearchToolbar;
    private HidelyImageButton btnScroll;
    private LinearLayout typingLayout;
    public ChatEditText etMessage;
    private RecordView recordView;
    private AnimButton recordButton;
    private androidx.constraintlayout.widget.Group searchGroup;
    private SearchView searchViewToolbar;
    private Toolbar toolbar;
    private CircleImageView userImgToolbarChatAct;
    private TextView userNameToolbarChatActivity, availableStatToolbar, tvCounterAction, tvTypingStatToolbar, countUnreadBadge;
    private AttachmentView attachmentView;
    private TextView tvCantSendMessages;
    private ConstraintLayout typingLayoutContainer, mainContainer;
    private LinearLayout imgAndBackContainer;

    //quoted message layout when replying
    private FrameLayout quotedMessageFrame;
    private View quotedColor;
    private EmojiTextView tvQuotedName;
    private EmojiTextView tvQuotedText;
    private ImageView quotedThumb;
    private ImageView btnCancelImage;


    View rootView;

    EmojiPopup emojiPopup;

    Menu currentMenu;

    RealmResults<Message> messageList;
    RealmResults<Message> observableList;


    OrderedRealmCollectionChangeListener<RealmResults<Message>> changeListener;

    private StickyHeaderDecoration decor;
    MessagingAdapter adapter;
    LinearLayoutManager linearLayoutManager;


    //to indicates the message from duplicate(onInsertions or onChange RealmListener)
    String previousMessageIdForScroll = "";
    String oldIdAudioPlayer = "";
    String timerStr = "";
    String presenceStat = "";
    String receiverUid;
    private Message currentQuotedMessage = null;

    GroupTyping groupTyping;


    boolean isLastSeenInitiated = false;
    boolean isTypedBefore = false;
    boolean wasInTypingMode = false;
    boolean typingStarted = false;
    boolean isGroup = false;
    boolean isBroadcast = false;

    private boolean mIsDetailsActivityStarted;
    private Bundle mTmpReenterState;

    int currentHeadsetState = -1;
    int oldPosAudioPlayer = 0;
    int searchIndex = 0;
    int unreadCount = 0;
    private int currentTypingState = TypingStat.NOT_TYPING;

    float initialToolbarTranslationY = 0;

    long presenceTimestamp = 0;

    User user;
    Chat chat;

    Recorder recorder;
    File recordFile;

    HeadsetReceiver headsetReceiver;
    IntentFilter headsetIntentFilter;

    ValueAnimator colorAnim;

    ValueEventListener messageStatListener, voiceMessageStatListener, typingStatListener, presenceStatListener;
    FireListener fireListener;

    FireManager fireManager;
    PerformCall performCall;
    private GroupManager groupManager = new GroupManager();

    private final SharedElementCallback mCallback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (mTmpReenterState != null) {
                int startingPosition = mTmpReenterState.getInt(EXTRA_STARTING_POSITION);

                int currentPosition = mTmpReenterState.getInt(EXTRA_CURRENT_ALBUM_POSITION);


                if (startingPosition != currentPosition) {
                    // If startingPosition != mCurrentPosition the user must have swiped to a
                    // different page in the DetailsActivity. We must update the shared element
                    // so that the correct one falls into place.
                    String newTransitionName = mTmpReenterState.getString(EXTRA_CURRENT_MESSAGE_ID);


                    View newSharedElement = recyclerView.findViewWithTag(newTransitionName);

                    if (newSharedElement != null) {
                        names.clear();
                        names.add(newTransitionName);
                        sharedElements.clear();
                        sharedElements.put(newTransitionName, newSharedElement);
                    }
                }

                mTmpReenterState = null;
            } else {
                // If mTmpReenterState is null, then the activity is exiting.
                View navigationBar = findViewById(android.R.id.navigationBarBackground);
                View statusBar = findViewById(android.R.id.statusBarBackground);
                if (navigationBar != null) {
                    names.add(ViewCompat.getTransitionName(navigationBar));
                    sharedElements.put(ViewCompat.getTransitionName(navigationBar), navigationBar);
                }
                if (statusBar != null) {
                    names.add(ViewCompat.getTransitionName(statusBar));
                    sharedElements.put(ViewCompat.getTransitionName(statusBar), statusBar);
                }
            }
        }
    };

    private ChatViewModel viewModel;
    private MessageSwipeController messageSwipeController;

    @Override
    public boolean enablePresence() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);


        init();
        setBackgroundImage();
        setSupportActionBar(toolbar);
        toolbar.setOverflowIcon(ContextCompat.getDrawable(getApplicationContext(),R.drawable.a_chat_ic_more));

        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        fireManager = new FireManager();
        //if user share something from external app to this app
        if (getIntent().hasExtra(IntentUtils.EXTRA_MIME_TYPE) && getIntent().hasExtra(IntentUtils.UID)) {
            String uid = getIntent().getStringExtra(IntentUtils.UID);
            user = RealmHelper.getInstance().getUser(uid);
            receiverUid = user.getUid();
            getChat();
            String mimeType = getIntent().getStringExtra(IntentUtils.EXTRA_MIME_TYPE);

            String filePath = null;
            if (getIntent().hasExtra(IntentUtils.EXTRA_REAL_PATH)) {
                filePath = getIntent().getStringExtra(IntentUtils.EXTRA_REAL_PATH);
            }


            switch (mimeType) {
                //text share
                case MimeTypes.TEXT_PLAIN:
                    String sharedText = getIntent().getStringExtra(IntentUtils.EXTRA_SHARED_TEXT);
                    changeSendButtonState(true);
                    etMessage.setText(sharedText);
                    break;

                //image share
                case MimeTypes.IMAGE:
                    //multiple images
                    if (getIntent().hasExtra(IntentUtils.EXTRA_REAL_PATH_LIST)) {
                        ArrayList<? extends String> imagesList = getIntent().getParcelableArrayListExtra(IntentUtils.EXTRA_REAL_PATH_LIST);
                        for (String path : imagesList) {
                            sendImage(path, false);
                        }
                        //one image
                    } else {
                        sendImage(filePath, false);
                    }
                    break;

                //video
                case MimeTypes.VIDEO:
                    sendTheVideo(filePath);
                    break;

                //audio
                case MimeTypes.AUDIO:
                    String length = Util.getVideoLength(this, filePath);
                    sendAudio(filePath, length);
                    break;

                //contact
                case MimeTypes.CONTACT:
                    List<ExpandableContact> selectedContacts = getIntent().getParcelableArrayListExtra(IntentUtils.EXTRA_CONTACT_LIST);
                    sendContacts(selectedContacts);
                    break;
            }


            //in case the user forwarded an image or a video from "FullScreenActivity"
        } else if (getIntent().hasExtra(IntentUtils.EXTRA_FORWARDED)) {
            String uid = getIntent().getStringExtra(IntentUtils.UID);
            user = RealmHelper.getInstance().getUser(uid);
            receiverUid = user.getUid();
            getChat();
            Message message = getIntent().getParcelableExtra(IntentUtils.EXTRA_MESSAGE);
            sendMessage(message);
        } else {
            //otherwise the user is coming from main activity
            //getting the user from realm because the thumb img may different from the parcelable
            String uid = getIntent().getStringExtra(IntentUtils.UID);
            user = RealmHelper.getInstance().getUser(uid);
            receiverUid = user.getUid();
            getChat();
        }

        isGroup = user.isGroupBool();
        isBroadcast = user.isBroadcastBool();

        if (isGroupNotNull()) {
            hideOrShowTypingLayout(user.getGroup().isActive());
            updateGroup();
        }


        loadMessagesList();

        setAdapter();


        fireListener = new FireListener();


        observeMessagesChanges();


        setUserInfoInToolbar();

        //animate exit animation from FullscreenActivity to this Activity
        setExitSharedElementCallback(mCallback);


        if (!isBroadcast) {
            listenForFriendStat();

            listenForTypingStat();

            listenForMessageStatListener();

            listenForVoiceMessageStatListener();
        }

        recordView.setCancelBounds(0);

        recordView.setSlideToCancelArrowColor(ContextCompat.getColor(this, R.color.iconTintColor));
        recordView.setCounterTimeColor(ContextCompat.getColor(this, R.color.colorText));
        recordView.setSlideToCancelTextColor(ContextCompat.getColor(this, R.color.colorText));
        recordButton.setRecordView(recordView);

        recordButton.setOnRecordClickListener(new OnRecordClickListener() {
            @Override
            public void onClick(View v) {
                if (user.isBlocked()) {
                    showBlockedDialog();
                    return;
                }
                String text = etMessage.getText().toString();
                sendMessage(text);
            }
        });
        recordView.setOnRecordListener(new OnRecordListener() {
            @Override
            public void onStart() {
                hideOrShowRecord(false);
                getDisposables().add(fireManager.setTypingStat(receiverUid, TypingStat.RECORDING, isGroup, isBroadcast).subscribe());
                handleRecord();
            }

            @Override
            public void onCancel() {
                stopRecord(true, -1);
                getDisposables().add(fireManager.setTypingStat(receiverUid, TypingStat.NOT_TYPING, isGroup, isBroadcast).subscribe());
            }

            @Override
            public void onFinish(long recordTime) {
                hideOrShowRecord(true);

                getDisposables().add(fireManager.setTypingStat(receiverUid, TypingStat.NOT_TYPING, isGroup, isBroadcast).subscribe());
                stopRecord(false, recordTime);
                requestEditTextFocus();
            }

            @Override
            public void onLessThanSecond() {
                Toast.makeText(ChatActivity.this, R.string.voice_message_is_short_toast, Toast.LENGTH_SHORT).show();
                hideOrShowRecord(true);
                getDisposables().add(fireManager.setTypingStat(receiverUid, TypingStat.NOT_TYPING, isGroup, isBroadcast).subscribe());
                stopRecord(true, -1);
                requestEditTextFocus();
            }
        });

        recordView.setOnBasketAnimationEndListener(new OnBasketAnimationEnd() {
            @Override
            public void onAnimationEnd() {
                hideOrShowRecord(true);
                requestEditTextFocus();
            }
        });


        btnScroll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scrollToLast();
            }
        });

        //check if user set preference Enter is Send
        if (SharedPreferencesManager.isEnterIsSend()) {
            etMessage.setImeOptions(EditorInfo.IME_ACTION_SEND);
            etMessage.setRawInputType(InputType.TYPE_CLASS_TEXT);
        }

        //onSendButton Click in keyboard
        etMessage.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage(etMessage.getText().toString());
                    return true;
                }
                return false;
            }
        });

        etMessage.setKeyBoardInputCallbackListener((inputContentInfo, flags, opts) -> {
            Uri contentUri = inputContentInfo.getContentUri();

            try {
                File file = File.createTempFile("temp", ".gif");
                boolean b = FileUtils.writeToFileFromContentUri(getContentResolver(), file, contentUri);
                if (b)
                    sendImage(file.getPath(), false);
                else {
                    Toast.makeText(ChatActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                }

            } catch (Exception e) {
                Toast.makeText(ChatActivity.this, R.string.error, Toast.LENGTH_SHORT).show();

                e.printStackTrace();
            }

        });


        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();

                if (text.trim().length() > 0) {
                    changeSendButtonState(true);
                    getDisposables().add(fireManager.setTypingStat(receiverUid, TypingStat.TYPING, isGroup, isBroadcast).subscribe());


                } else if (text.trim().length() == 0 && typingStarted) {
                    changeSendButtonState(false);
                    if (!isBroadcast)
                        getDisposables().add(fireManager.setTypingStat(receiverUid, TypingStat.NOT_TYPING, isGroup, isBroadcast).subscribe());
                }
            }
        });


        etMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emojiPopup.dismiss();
                if (attachmentView.isShowing())
                    attachmentView.hide(imgAttachment);

            }
        });

        imgAttachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (user.isBlocked()) {
                    showBlockedDialog();
                } else {
                    attachmentView.reveal(view);
                    KeyboardHelper.hideSoftKeyboard(ChatActivity.this, etMessage);
                }

            }
        });


        attachmentView.setOnAttachmentClick(new AttachmentView.AttachmentClickListener() {
            @Override
            public void OnClick(int id) {
                switch (id) {
                    case R.id.attachment_gallery:
                        pickImages();
                        break;

                    case R.id.attachment_camera:
                        startCamera();
                        break;

                    case R.id.attachment_document:
                        pickFile();
                        break;

                    case R.id.attachment_audio:
                        pickMusic();
                        break;

                    case R.id.attachment_contact:
                        pickContact();
                        break;

                    case R.id.attachment_location:
                        pickLocation();
                        break;
                }
            }
        });


        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                //detect when user stops scrolling
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {

                    int lastVisibleItemPosition = getLastVisibileItem();

                    if (lastVisibleItemPosition != messageList.size() - 1) {
                        //only show it when it is hidden
                        if (!btnScroll.isShowing())
                            btnScroll.show();

                    } else {
                        btnScroll.hide();
                        hideUnreadCount();

                    }
                }
            }

        });


        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (user.isBlocked()) {
                    showBlockedDialog();
                    return;
                }

                startCamera();
            }
        });

        emojiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emojiPopup.toggle();
            }
        });

        imgAndBackContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        btnToolbarBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });


        searchViewToolbar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(final String query) {

                final RealmResults<Message> results = RealmHelper.getInstance().searchForMessage(receiverUid, query);


                if (!results.isEmpty()) {

                    //get the found last message index
                    searchIndex = results.size() - 1;
                    String foundMessageId = results.get(searchIndex).getMessageId();
                    int mIndex = getPosFromId(foundMessageId);


                    scrollAndHighlightSearch(mIndex);


                    downArrowSearchToolbar.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //+2 because one for index and one for previous
                            //check if there are another results
                            if (results.isEmpty() || searchIndex + 2 > results.size()) {
                                Toast.makeText(ChatActivity.this, R.string.not_found, Toast.LENGTH_SHORT).show();
                                return;
                            }

                            //increment current index
                            searchIndex++;

                            String foundMessageId = results.get(searchIndex).getMessageId();
                            //get the index from chatList by message id from searchedList
                            int mIndex = getPosFromId(foundMessageId);

                            scrollAndHighlightSearch(mIndex);


                        }
                    });

                    upArrowSearchToolbar.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            if (results.isEmpty() || searchIndex - 1 < 0) {
                                Toast.makeText(ChatActivity.this, R.string.not_found, Toast.LENGTH_SHORT).show();
                                return;
                            }


                            //decrement search index
                            searchIndex -= 1;

                            String foundMessageId = results.get(searchIndex).getMessageId();
                            int mIndex = getPosFromId(foundMessageId);


                            scrollAndHighlightSearch(mIndex);


                        }
                    });
                }

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });


        etMessage.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                wasInTypingMode = !hasFocus;
            }
        });


        searchViewToolbar.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b)
                    KeyboardHelper.openSoftKeyboard(ChatActivity.this, view.findFocus());
            }
        });

        searchViewToolbar.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                isInSearchMode = false;
                return true;
            }
        });

        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isInActionMode || isInSearchMode) return;
                viewContact();
            }
        });


        if (getResources().getBoolean(R.bool.is_interstitial_ad_enabled))
            loadInterstitialAd();


        btnCancelImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideReplyLayout();
            }
        });

        //set quoted message colors
        setQuotedMessageStyle();

        performCall = new PerformCall(this, fireManager, getDisposables());

        viewModel.getItemSelectedLiveData().observe(this, selectedMessages -> {
            if (selectedMessages.isEmpty())
                exitActionMode();

            else {
                updateToolbarButtons(selectedMessages);
            }

            updateActionModeItemsCount(selectedMessages.size());

        });
    }


    //hide or show toolbar button in activity depending on conditions
    private void updateToolbarButtons(List<Message> selectedMessages) {
        if (AdapterHelper.shouldHideAllItems(selectedMessages)) {
            hideShareItem();
            hideCopyItem();
            hideForwardItem();
            hideReplyMenuItem();
        } else {
            if (AdapterHelper.shouldEnableCopyItem(selectedMessages))
                showCopyItem();
            else
                hideCopyItem();

            if (AdapterHelper.shouldEnableForwardButton(selectedMessages))
                showForwardItem();
            else
                hideForwardItem();

            if (AdapterHelper.shouldEnableShareButton(selectedMessages))
                showShareItem();
            else
                hideShareItem();

            boolean isGroupActive = isGroupActive();
            if (AdapterHelper.shouldEnableReplyItem(selectedMessages, user.isGroupBool(), isGroupActive))
                showReplyItem();
            else
                hideReplyMenuItem();
        }
    }

    private boolean isGroupActive() {
        return user.getGroup() != null && user.getGroup().isActive();
    }

    private void setQuotedMessageStyle() {
        quotedMessageFrame.setBackgroundColor(ContextCompat.getColor(this, R.color.quoted_sent_background_color));
        tvQuotedName.setTextColor(ContextCompat.getColor(this, R.color.quoted_sent_text_color));
        quotedColor.setBackgroundColor(ContextCompat.getColor(this, R.color.quoted_sent_quoted_color));
        btnCancelImage.setColorFilter(ContextCompat.getColor(this, R.color.quoted_cancel_color), PorterDuff.Mode.SRC_IN);
    }

    //set background image if user chooses another image than the default one
    private void setBackgroundImage() {
        if (SharedPreferencesManager.getWallpaperPath().equals(""))
            return;

        Bitmap bitmap;
        try {
            bitmap = BitmapUtils.convertFileImageToBitmap(SharedPreferencesManager.getWallpaperPath());
            if (bitmap != null)
                mainContainer.setBackground(new BitmapDrawable(null, bitmap));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.failed_to_load_wallpaper, Toast.LENGTH_SHORT).show();
        }

    }


    private void updateGroup() {
        getDisposables().add(groupManager.updateGroup(user.getUid(), null).subscribe());
    }

    private void hideOrShowTypingLayout(boolean show) {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(mainContainer);
        if (show) {
            //when group is ONLY active ,then listen for typing users
            groupTyping = new GroupTyping(user.getGroup().getUsers(), receiverUid, this);
            constraintSet.connect(R.id.recycler_chat, ConstraintSet.BOTTOM, R.id.quoted_message_frame, ConstraintSet.TOP);
            constraintSet.applyTo(mainContainer);
            typingLayoutContainer.setVisibility(View.VISIBLE);
            tvCantSendMessages.setVisibility(View.GONE);

        } else {
            constraintSet.connect(R.id.recycler_chat, ConstraintSet.BOTTOM, R.id.tv_cant_send_messages, ConstraintSet.TOP);
            constraintSet.applyTo(mainContainer);
            typingLayoutContainer.setVisibility(View.GONE);
            tvCantSendMessages.setVisibility(View.VISIBLE);
        }


    }

    private boolean isGroupNotNull() {
        return isGroup && user.getGroup() != null && user.getGroup().getUsers() != null;
    }

    private void loadInterstitialAd() {
        final InterstitialAd interstitialAd = new InterstitialAd(this);
        interstitialAd.setAdUnitId(getString(R.string.interstitial_ad_id));
        interstitialAd.loadAd(new AdRequest.Builder().build());
        interstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                interstitialAd.show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        EventBus.getDefault().register(this);

        registerReceiver(headsetReceiver, headsetIntentFilter);


        addFireListeners();
        updateProgressData();
        updateIncomingMessagesState();
        updateSentMessagesState();
        observableList.addChangeListener(changeListener);


        mIsDetailsActivityStarted = false;


        MyApp.chatActivityResumed(receiverUid);
        startUpdatePresenceTask();


        // update contact info if clicked  addToContacts
        if (!isGroup && !isBroadcast && ContactUtils.contactExists(this, user.getPhone()) && currentMenu != null) {
            if (currentMenu.findItem(R.id.add_to_contacts) != null) {
                currentMenu.findItem(R.id.add_to_contacts).setVisible(false);
                userNameToolbarChatActivity.setText(ContactUtils.queryForNameByNumber(user.getPhone()));
            }
        }

        if (!isBroadcast)
            getUserPhoto();

        //dismiss notification
        new NotificationHelper(this).dismissNotification(receiverUid, true);

        if (isGroup) {
            //update group info if something has changed
            updateGroupEvent(new UpdateGroupEvent(user.getUid()));

        }

    }

    @Override
    protected void onPause() {
        //check if audio is played before
        if (!oldIdAudioPlayer.equals("")) {
            //stop audio when app is not in foreground
            ServiceHelper.stopAudio(this);
            //update UI with paused state
            onAudioPause(new AudioServiceCallbacksEvent.onPause(oldIdAudioPlayer, oldPosAudioPlayer));
        }

        currentHeadsetState = -1;
        unregisterReceiver(headsetReceiver);
        observableList.removeChangeListener(changeListener);
        removeFireListeners();
        MyApp.chatActivityPaused();
        stopUpdatePresenceTask();

        EventBus.getDefault().unregister(this);
        super.onPause();

    }

    @Override
    protected void onStop() {
        super.onStop();
        getDisposables().add(fireManager.setTypingStat(receiverUid, TypingStat.NOT_TYPING, isGroup, isBroadcast).subscribe());
    }

    @Override
    protected void onDestroy() {
        stopService(new Intent(this, AudioService.class));
        super.onDestroy();
    }


    private void getChat() {
        chat = RealmHelper.getInstance().getChat(receiverUid);
    }


    private void viewContact() {
        Intent intent = new Intent(ChatActivity.this, UserDetailsActivity.class);
        intent.putExtra(IntentUtils.UID, user.getUid());
        String transitionName = getResources().getString(R.string.profile_translation_name);
        startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(this, userImgToolbarChatAct, transitionName).toBundle());
    }


    private void changeSendButtonState(boolean setTyping) {
        if (setTyping) {
            recordButton.goToState(AnimButton.TYPING_STATE);
            recordButton.setListenForRecord(false);
            typingStarted = true;

        } else {
            recordButton.goToState(AnimButton.RECORDING_STATE);
            recordButton.setListenForRecord(true);
            typingStarted = false;


        }

    }

    private void startCamera() {
        startActivityForResult(new Intent(ChatActivity.this, CameraActivity.class), CAMERA_REQUEST);
    }

    private void scrollAndHighlightSearch(final int index) {
        recyclerView.scrollToPosition(index);
        View view = this.getCurrentFocus();
        //hide keyboard
        if (view != null)
            KeyboardHelper.hideSoftKeyboard(this, view);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //get view holder of this textView
                RecyclerView.ViewHolder viewHolderForAdapterPosition = recyclerView.findViewHolderForAdapterPosition(index);
                //get textView
                TextView tv = viewHolderForAdapterPosition.itemView.findViewById(R.id.tv_message_content);
                //highlight text
                tv.setText(Util.highlightText(tv.getText().toString()));
            }
        }, 100);

    }

    private void scrollAndHighlightQuotedMessage(final int index) {
        recyclerView.scrollToPosition(index);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //get view holder of this textView
                RecyclerView.ViewHolder viewHolderForAdapterPosition = recyclerView.findViewHolderForAdapterPosition(index);
                if (viewHolderForAdapterPosition != null) {
                    //get textView
                    animateQuotedMessageBg(viewHolderForAdapterPosition.itemView);
                }
                //highlight text
            }
        }, 100);

    }

    //Update sent Messages Changes When the listener is not alive
    private void updateSentMessagesState() {
        if (isGroup || isBroadcast) return;
        RealmResults<Message> unreadMessages = RealmHelper.getInstance().getUnreadAndUnDeliveredSentMessages(receiverUid, FireManager.getUid());
        for (final Message unreadMessage : unreadMessages) {
            //if it's a broadcast message then get the ACTUAL message id
            //since we are ONLY copying the message locally

            DatabaseReference ref = FireConstants.messageStat.child(receiverUid).child(unreadMessage.getMessageId());
            fireListener.addListener(ref, messageStatListener);
        }


        RealmResults<Message> unReadVoiceMessages = RealmHelper.getInstance().getUnReadVoiceMessages(receiverUid);

        for (Message unReadVoiceMessage : unReadVoiceMessages) {
            //if it's a broadcast message then get the ACTUAL message id
            //since we are ONLY copying the message locally

            DatabaseReference ref = FireConstants.voiceMessageStat.child(receiverUid).child(unReadVoiceMessage.getMessageId());
            fireListener.addListener(ref, voiceMessageStatListener);
        }
    }


    //update user photo thumb img
    private void getUserPhoto() {
        if (!NetworkHelper.isConnected(this)) return;
        getDisposables().add(fireManager.checkAndDownloadUserThumbImg(user).subscribe(thumbImg -> {
            Glide.with(ChatActivity.this).load(user.getThumbImg()).into(userImgToolbarChatAct);
        }, throwable -> {

        }));
    }


    //transition effects
    @Override
    public void onActivityReenter(int requestCode, Intent data) {
        super.onActivityReenter(requestCode, data);
        supportPostponeEnterTransition();
        recyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                recyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                recyclerView.requestLayout();
                supportStartPostponedEnterTransition();
                return true;
            }
        });
    }


    //listen for typing or recording status
    private void listenForTypingStat() {
        typingStatListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null)
                    return;

                currentTypingState = dataSnapshot.getValue(Integer.class);

                if (currentTypingState == TypingStat.NOT_TYPING && isLastSeenInitiated && isTypedBefore) {

                    tvTypingStatToolbar.setText(TypingStat.getStatString(ChatActivity.this, currentTypingState));
                    updateToolbarTvsVisibility(false);


                } else if (currentTypingState == TypingStat.RECORDING || currentTypingState == TypingStat.TYPING) {
                    tvTypingStatToolbar.setText(TypingStat.getStatString(ChatActivity.this, currentTypingState));
                    updateToolbarTvsVisibility(true);
                }

                isTypedBefore = true;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };


    }

    private void updateToolbarTvsVisibility(boolean hideOnlineStatToolbar) {
        if (isInActionMode || isInSearchMode) return;


        if (isGroup || isBroadcast) {
            if (hideOnlineStatToolbar) {
                tvTypingStatToolbar.setVisibility(View.VISIBLE);
                availableStatToolbar.setVisibility(View.GONE);
            } else {
                availableStatToolbar.setVisibility(View.VISIBLE);
                tvTypingStatToolbar.setVisibility(View.GONE);
            }
        } else {
            if (hideOnlineStatToolbar) {
                tvTypingStatToolbar.setVisibility(View.VISIBLE);
                availableStatToolbar.setVisibility(View.GONE);
            } else {
                tvTypingStatToolbar.setVisibility(View.GONE);
                if (presenceStat.equals(""))
                    availableStatToolbar.setVisibility(View.GONE);

                else
                    availableStatToolbar.setVisibility(View.VISIBLE);
            }
        }

        float spacing = DpUtil.toPixel(3, this);

        if (isGroup)
            userNameToolbarChatActivity.animate().translationY(initialToolbarTranslationY - spacing).start();

//        animate online tv
        else if (availableStatToolbar.getText().toString().equals("") && tvTypingStatToolbar.getText().toString().equals("")) {
            userNameToolbarChatActivity.animate().translationY(initialToolbarTranslationY + spacing).start();

        } else {
//            do not move it up unless it's down
            if (initialToolbarTranslationY - spacing != initialToolbarTranslationY) {
                if (presenceStat.equals("") && currentTypingState == TypingStat.NOT_TYPING) return;

                userNameToolbarChatActivity.animate().translationY(initialToolbarTranslationY - spacing).start();
                availableStatToolbar.animate().translationY(initialToolbarTranslationY - spacing).start();
                tvTypingStatToolbar.animate().translationY(initialToolbarTranslationY - spacing).start();
            }
        }


    }

    //add firebase Listeners
    private void addFireListeners() {
        if (!isGroup && !isBroadcast) {
            DatabaseReference typingStatRef = FireConstants.mainRef.child("typingStat")
                    .child(receiverUid).child(FireManager.getUid());
            fireListener.addListener(typingStatRef, typingStatListener);
            fireListener.addListener(presenceRef.child(receiverUid), presenceStatListener);
        }
    }


    //remove fire listeners to clean up resources and avoid memory leaks
    private void removeFireListeners() {
        fireListener.cleanup();
        if (groupTyping != null)
            groupTyping.cleanUp();
    }


    //listen for friend status and see if he is online ,otherwise set last seen time
    private void listenForFriendStat() {
        presenceStatListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) {
                    return;
                }


                if (dataSnapshot.getValue() instanceof String) {

                    presenceStat = dataSnapshot.getValue(String.class);

                    if (presenceStat.equalsIgnoreCase("Online")) {
                        availableStatToolbar.setText(R.string.online);
                        updateToolbarTvsVisibility(false);
                        presenceStat = "Online";
                    }
                } else {
                    long timestamp = dataSnapshot.getValue(Long.class);
                    presenceTimestamp = timestamp;
                    presenceStat = TimeHelper.getTimeAgo(timestamp);
                    availableStatToolbar.setText(presenceStat);
                    updateToolbarTvsVisibility(false);
                }

                isLastSeenInitiated = true;

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

    }

    //set incoming messages to read
    private void updateIncomingMessagesState() {
        if (isBroadcast) return;

        if (isGroup) {
            //set received messages as read
            RealmHelper.getInstance().setMessagesAsReadLocally(receiverUid);
        } else {
            //update received messages as read in Server
            fireManager.setMessagesAsRead(this, receiverUid);
        }
    }

    private void addListener(String messageId) {
        if (isBroadcast) return;
        DatabaseReference ref = FireConstants.messageStat.child(receiverUid).child(messageId);
        fireListener.addListener(ref, messageStatListener);
    }


    private void addVoiceMessageStatListener(final String messageId) {
        if (isBroadcast) return;
        DatabaseReference ref = FireConstants.voiceMessageStat.child(receiverUid).child(messageId);
        fireListener.addVoiceMessageListener(ref, voiceMessageStatListener);
    }

    //if message is sent was RECEIVED or READ by the Receiver then we will update its state to the database
    private void listenForMessageStatListener() {
        messageStatListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null)
                    return;


                int val = dataSnapshot.getValue(Integer.class);
                String messageId = dataSnapshot.getKey();
                RealmHelper.getInstance().updateMessageStatLocally(messageId, user.getUid(), val);
                if (val == MessageStat.READ && fireListener != null) {
                    //free up resources since this will not be updated anymore!
                    fireListener.removeListener(dataSnapshot.getRef(), messageStatListener);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

    }

    //if message is sent was heard by the Receiver then we will update its state to the database
    private void listenForVoiceMessageStatListener() {
        voiceMessageStatListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null)
                    return;


                String messageId = dataSnapshot.getKey();

                RealmHelper.getInstance().updateVoiceMessageStatLocally(messageId, user.getUid());


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }


    //this is called **whenever** a CHANGE occurs to the "observableList" ,insertion,change,delete,etc...
    private void observeMessagesChanges() {
        changeListener = new OrderedRealmCollectionChangeListener<RealmResults<Message>>() {
            @Override
            public void onChange(RealmResults<Message> messages, OrderedCollectionChangeSet changeSet) {

                OrderedCollectionChangeSet.Range[] modifications = changeSet.getChangeRanges();
                OrderedCollectionChangeSet.Range[] insertion = changeSet.getInsertionRanges();


                if (insertion.length != 0) {
                    updateChat(messages.get(insertion[0].startIndex));

                } else if (modifications.length != 0) {

                    updateChat(messages.get(modifications[0].startIndex));
                }


                for (OrderedCollectionChangeSet.Range range : modifications) {
                    //get the new Message
                    final Message message = messages.get(range.startIndex);


                    //if this message was sent by user then we want to add a Listener to it to observe newChanges to its state whether it's RECEIVED OR READ
                    if (!isBroadcast && message.getType() != MessageType.GROUP_EVENT && message.getFromId().equals(FireManager.getUid()))
                        //if it's a broadcast message then get the ACTUAL message id
                        //since we are ONLY copying the message locally
                        addListener(message.getMessageId());


                    //update date header if it's a new day
                    adapter.messageInserted();


                    //update incoming messages
                    // if this message is from the recipient and its' not read before then update the message currentTypingState to READ
                    if (!isGroup && message.getType() != MessageType.GROUP_EVENT && !message.getFromId().equals(FireManager.getUid()) && message.getChatId().equals(receiverUid) && message.getMessageStat() != MessageStat.READ) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                ServiceHelper.startUpdateMessageStatRequest(ChatActivity.this, message.getMessageId(), fireManager.getUid(), message.getChatId(), MessageStat.READ);
                            }
                        }, 100);

                    }

                }
            }
        };
    }

    //scroll to last OR update the unread count
    private void updateChat(Message message) {

        if (message.getType() == MessageType.GROUP_EVENT)
            return;

        //if the message was send by the user then scroll to last
        if (message.getFromId().equals(FireManager.getUid()) && message.getMessageStat() == MessageStat.PENDING) {
            scrollToLast();
        } else {
            //if the message was sent by Receiver and its state is still pending
            if (message.getChatId().equals(receiverUid) && message.getMessageStat() == MessageStat.PENDING) {
                //get index from the message
                int i = messageList.indexOf(message);
                //if it's -1 (not exists) return
                if (i == -1)
                    return;


                //get last visible item on screen
                int lastVisibleItemPosition = getLastVisibileItem();

                //if the last message is visible then we will scroll to last
                //the user in this case is at before the last message that inserted
                // therefore a new message was inserted and we want to scroll to it
                //"-2" because one for index and one for previous message
                if (messageList.size() - 2 == lastVisibleItemPosition) {
                    scrollToLast();
                } else {
                    //otherwise the user may was checking another messages
                    //and for that we want to show the unreadCount badge with the count
                    if (lastVisibleItemPosition != i && !message.getMessageId().equals(previousMessageIdForScroll) && message.getType() != MessageType.GROUP_EVENT) {
                        unreadCount++;
                        countUnreadBadge.setText(unreadCount + "");
                        countUnreadBadge.setVisibility(View.VISIBLE);
                        btnScroll.show();
                    }
                    previousMessageIdForScroll = message.getMessageId();
                }
            }
        }
    }


    //stop record
    private void stopRecord(boolean isCancelled, long recordTime) {
        try {
            if (recorder != null)
                recorder.stopRecording();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //if it's cancelled (the user swiped to cancel) then delete the recordFile
        if (isCancelled) {
            recordFile.delete();
        } else {
            if (user.isBlocked()) {
                recordFile.delete();
                showBlockedDialog();
                return;
            }
            //otherwise get the recordTime and convert it to Readable String and send the message
            timerStr = Util.milliSecondsToTimer(recordTime);
            String filePath = recordFile.getPath();
            sendVoiceMessage(filePath, timerStr);
        }

    }


    //start recording voice message
    private void handleRecord() {
        recordFile = DirManager.generateFile(MessageType.SENT_VOICE_MESSAGE);
        recorder = OmRecorder.wav(
                new PullTransport.Default(RecorderSettings.getMic(), new PullTransport.OnAudioChunkPulledListener() {
                    @Override
                    public void onAudioChunkPulled(AudioChunk audioChunk) {

                    }
                }), recordFile);


        //start record when the record sound "BEEP" finishes
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                recorder.startRecording();
            }
        }, RECORD_START_AUDIO_LENGTH);


    }


    private void scrollToLast() {
        if (messageList == null) return;
        if (messageList.size() - 1 <= 0) return;


        recyclerView.scrollToPosition(messageList.size() - 1);
        hideUnreadCount();
        btnScroll.hide();
    }


    private void setAdapter() {

        adapter = new MessagingAdapter(messageList, true, this, this,
                user, SharedPreferencesManager.getThumbImg(),
                viewModel.getItemSelectedLiveData(), viewModel.getProgressMapLiveData(), viewModel.getAudibleState());


        decor = new StickyHeaderDecoration(adapter);
        linearLayoutManager = new LinearLayoutManager(this);

        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);

        //add Date Header to the Adapter
        recyclerView.addItemDecoration(decor, 0);

        //if there are messages in this chat
        if (messageList.size() > 0) {

            Chat chat = RealmHelper.getInstance().getChat(receiverUid);

            //if there are unread messages
            if (chat.getUnReadCount() != 0 && !chat.getUnreadMessages().isEmpty()) {

                //get the firs unreade message index in list
                Message firstUnreadMessage = chat.getUnreadMessages().first();
                if (firstUnreadMessage != null) {
                    int firstUnreadMessageIndex = messageList.indexOf(firstUnreadMessage);

                    //if it's exists then scroll to it
                    if (firstUnreadMessageIndex != -1)
                        recyclerView.scrollToPosition(firstUnreadMessageIndex);
                }

                //otherwise scroll to last message
            } else {
                recyclerView.scrollToPosition(messageList.size() - 1);
            }

            enableSwipeToDeleteAndUndo();
        }


    }


    private void enableSwipeToDeleteAndUndo() {

        messageSwipeController = new MessageSwipeController(this, isGroupActive(), new SwipeControllerActions() {
            @Override
            public void showReplyUI(int position) {
                replyItemClicked(messageList.get(position));
            }
        });


        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(messageSwipeController);
        itemTouchHelper.attachToRecyclerView(recyclerView);

    }

    //init views
    private void init() {
        recyclerView = findViewById(R.id.recycler_chat);
        rootView = findViewById(R.id.root_view);
        emojiBtn = findViewById(R.id.emoji_btn);
        etMessage = findViewById(R.id.et_message);
        imgAttachment = findViewById(R.id.img_attachment);
        cameraBtn = findViewById(R.id.camera_btn);
        recordView = findViewById(R.id.record_view);
        recordButton = findViewById(R.id.record_button);
        typingLayout = findViewById(R.id.typing_layout);

        emojiPopup = EmojiPopup.Builder.fromRootView(rootView)
                .setOnEmojiPopupShownListener(() -> emojiBtn.setImageResource(R.drawable.ic_baseline_keyboard_24))
                .setOnEmojiPopupDismissListener(() -> emojiBtn.setImageResource(R.drawable.ic_insert_emoticon_black))
                .build(etMessage);


        toolbar = findViewById(R.id.toolbar);
        userImgToolbarChatAct = findViewById(R.id.user_img_toolbar_chat_act);
        userNameToolbarChatActivity = findViewById(R.id.user_name_toolbar_chat_activity);
        availableStatToolbar = findViewById(R.id.available_stat_toolbar);
        tvTypingStatToolbar = findViewById(R.id.tv_typing_stat_toolbar);
        tvCounterAction = findViewById(R.id.tv_counter_action);
        attachmentView = findViewById(R.id.attachment_view);
        btnToolbarBack = findViewById(R.id.btn_toolbar_back);
        searchViewToolbar = findViewById(R.id.search_view_toolbar);
        upArrowSearchToolbar = findViewById(R.id.up_arrow_search_toolbar);
        downArrowSearchToolbar = findViewById(R.id.down_arrow_search_toolbar);
        btnScroll = findViewById(R.id.btn_scroll);
        countUnreadBadge = findViewById(R.id.count_unread_badge);
        tvCantSendMessages = findViewById(R.id.tv_cant_send_messages);
        typingLayoutContainer = findViewById(R.id.typing_layout_container);
        mainContainer = findViewById(R.id.content_chat);
        searchGroup = findViewById(R.id.search_layout);

        quotedMessageFrame = findViewById(R.id.quoted_message_frame);
        quotedColor = findViewById(R.id.quoted_color);
        tvQuotedName = findViewById(R.id.tv_quoted_name);
        tvQuotedText = findViewById(R.id.tv_quoted_text);
        quotedThumb = findViewById(R.id.quoted_thumb);
        btnCancelImage = findViewById(R.id.btn_cancel_image);

        imgAndBackContainer = findViewById(R.id.img_and_back_container);


        headsetReceiver = new HeadsetReceiver();
        headsetIntentFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);

    }

    private void hideUnreadCount() {
        unreadCount = 0;
        countUnreadBadge.setText("");
        countUnreadBadge.setVisibility(View.GONE);
    }

    // hide/show typingLayout or recordLayout
    private void hideOrShowRecord(boolean hideRecord) {
        if (hideRecord) {
            recordView.setVisibility(View.GONE);
            typingLayout.setVisibility(View.VISIBLE);
        } else {
            recordView.setVisibility(View.VISIBLE);
            typingLayout.setVisibility(View.GONE);
        }
    }

    //set the cursor on the EditText after finish recording
    private void requestEditTextFocus() {
        if (wasInTypingMode) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    etMessage.requestFocus();
                }
            }, 100);

        }
    }


    private void setUserInfoInToolbar() {
        if (user.getThumbImg() != null)
            Glide.with(ChatActivity.this).load(user.getThumbImg()).into(userImgToolbarChatAct);
        else {
            if (user.isBroadcastBool())
                userImgToolbarChatAct.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_broadcast_with_bg));
        }

        if (isGroup || isBroadcast) {

            updateToolbarTvsVisibility(false);

            setMembersInToolbar();
        }

        userNameToolbarChatActivity.setText(user.getProperUserName());

    }

    //set group members names in toolbar and separate them by ","
    private void setMembersInToolbar() {
        String names = "";
        String SEPARATOR = " , ";

        List<User> users = isGroup ? user.getGroup().getUsers() : user.getBroadcast().getUsers();
        for (User user : users) {
            if (user.getUid().equals(FireManager.getUid())) {
                names += getResources().getString(R.string.you) + SEPARATOR;
            } else
                names += user.getProperUserName() + SEPARATOR;
        }
        String userNames = StringUtils.removeExtraSeparators(names, SEPARATOR);
        availableStatToolbar.setText(userNames);


    }


    private void pickImages() {
        Matisse.from(ChatActivity.this)
                .choose(MimeType.of(MimeType.MP4, MimeType.THREEGPP, MimeType.THREEGPP2
                        , MimeType.JPEG, MimeType.BMP, MimeType.PNG, MimeType.GIF))
                .countable(true)
                .maxSelectable(MAX_SELECTABLE)
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                .thumbnailScale(0.85f)
                .imageEngine(new GlideEngine())
                .forResult(PICK_GALLERY_REQUEST);
    }


    private void pickMusic() {
        Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, PICK_MUSIC_REQUEST);
    }

    private void pickLocation() {
        startActivityForResult(new Intent(this, PlacesPickerActivity.class), PICK_LOCATION_REQUEST);
    }

    private void pickContact() {
        new MultiContactPicker.Builder(ChatActivity.this)
                .handleColor(ContextCompat.getColor(ChatActivity.this, R.color.colorPrimary))
                .bubbleColor(ContextCompat.getColor(ChatActivity.this, R.color.colorPrimary))
                .showPickerForResult(PICK_CONTACT_REQUEST);
    }

    private void pickFile() {
        StorageChooser chooser = new StorageChooser.Builder()
                .withActivity(ChatActivity.this)
                .withFragmentManager(getFragmentManager())
                .allowCustomPath(true)
                .setType(StorageChooser.FILE_PICKER)
                .disableMultiSelect()
                .build();


        chooser.show();


        chooser.setOnSelectListener(new StorageChooser.OnSelectListener() {
            @Override
            public void onSelect(String path) {
                File file = new File(path);
                int file_size = Integer.parseInt(String.valueOf(file.length() / 1024));
                String fileExtension = Util.getFileExtensionFromPath(path);

                if (file_size > MAX_FILE_SIZE) {
                    Toast.makeText(ChatActivity.this, R.string.file_is_too_big, Toast.LENGTH_SHORT).show();

                } else if (!FileFilter.isOkExtension(fileExtension)) {
                    Toast.makeText(ChatActivity.this, R.string.type_not_supported, Toast.LENGTH_SHORT).show();
                } else {
                    sendFile(path);
                }

            }
        });


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.currentMenu = menu;

        getMenuInflater().inflate(R.menu.menu_chat, menu);

        updateBlockMenuItemTitle(menu);

        if (!isGroup && !isBroadcast)
            menu.findItem(R.id.add_to_contacts).setVisible(!ContactUtils.contactExists(this, user.getPhone()));
        else {
            if (user.isBroadcastBool()) {
                menu.findItem(R.id.voice_call_item).setVisible(false);
                menu.findItem(R.id.video_call_item).setVisible(false);
            }

            if (user.isGroupBool()) {
                updateGroupCallButtonsVisibility(menu);
            }
        }


        if (chat == null) {
            MenuItem item = menu.findItem(R.id.mute_item);
            if (item != null) {
                item.setVisible(false);
            }
        }

        updateMuteItemTitle();

        setGroupMenuItems(menu);


        return super.onCreateOptionsMenu(menu);
    }

    private void updateGroupCallButtonsVisibility(Menu menu) {

        boolean showCallsButtons = isGroupActive() && user.getGroup() != null && user.getGroup().getUsers().size() <= R.integer.max_group_call_count;

        MenuItem voiceCallItem = menu.findItem(R.id.voice_call_item);
        if (voiceCallItem != null) {
            voiceCallItem.setVisible(showCallsButtons);
        }

        MenuItem videoCallItem = menu.findItem(R.id.video_call_item);
        if (videoCallItem != null) {
            videoCallItem.setVisible(showCallsButtons);
        }
    }

    private void setGroupMenuItems(Menu menu) {
        if (isGroup || isBroadcast) {
            menu.findItem(R.id.block_contact).setVisible(false);
            menu.findItem(R.id.add_to_contacts).setVisible(false);
            if (isGroup)
                menu.findItem(R.id.view_contact_menu_item).setTitle(R.string.group_info);
            else menu.findItem(R.id.view_contact_menu_item).setTitle(R.string.broadcast_list_info);

        }
    }

    private void updateBlockMenuItemTitle(Menu menu) {
        if (menu == null) return;
        MenuItem item = menu.findItem(R.id.block_contact);
        if (item != null) {
            item.setTitle(user.isBlocked() ? getString(R.string.unblock_contact) : getString(R.string.block_contact));
        }
    }

    private void setMuted() {
        if (chat != null) {
            RealmHelper.getInstance().setMuted(chat.getChatId(), !chat.isMuted());
            updateMuteItemTitle();
        }
    }

    private void updateMuteItemTitle() {
        if (toolbar.getMenu().findItem(R.id.mute_item) != null) {
            toolbar.getMenu().findItem(R.id.mute_item).setTitle(chat != null && chat.isMuted() ? getString(R.string.unmute_notifications) : getString(R.string.mute_notifications));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.video_call_item:
                if (user.isGroupBool()) {
                    if (user.getGroup() != null && user.getGroup().isActive()) {
                        performCall.performConferenceCall(true, user.getUid());
                    }
                } else
                    performCall.performCall(true, receiverUid);
                break;

            case R.id.voice_call_item:
                if (user.isGroupBool()) {
                    if (user.getGroup() != null && user.getGroup().isActive()) {
                        performCall.performConferenceCall(false, user.getUid());
                    }
                } else
                    performCall.performCall(false, receiverUid);
                break;

            case R.id.view_contact_menu_item:
                viewContactItemClicked();
                break;

            case R.id.menu_item_copy:
                copyItemClicked();
                break;

            case R.id.menu_item_delete:
                deleteItemClicked();
                break;

            case R.id.menu_item_share:
                shareClicked();
                break;


            case R.id.menu_item_forward:
                forwardClicked();
                break;

            case R.id.search_item:
                searchItemClicked();
                break;

            case R.id.block_contact:
                blockUserClicked();
                break;

            case R.id.add_to_contacts:
                addToContacts();
                break;

            case R.id.mute_item:
                setMuted();
                break;

            case R.id.clear_chat_item:
                clearChat();
                break;

            case R.id.menu_item_reply:
                replyItemClicked();
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    private void replyItemClicked() {
        Message selectedMessage = viewModel.getSelectedItems().get(0);
        if (selectedMessage == null) return;
        String author;
        if (selectedMessage.getFromId().equals(FireManager.getUid())) {
            author = getResources().getString(R.string.you);
        } else {
            User user = RealmHelper.getInstance().getUser(selectedMessage.getFromId());
            author = user == null ? selectedMessage.getFromPhone() : user.getProperUserName();
        }

        showReplyLayout(author, selectedMessage);
        exitActionMode();
        KeyboardHelper.openSoftKeyboard(this, etMessage.findFocus());
        etMessage.requestFocus();

        currentQuotedMessage = selectedMessage;
    }

    private void replyItemClicked(Message selectedMessage) {

        if (selectedMessage == null) return;
        String author;
        if (selectedMessage.getFromId().equals(FireManager.getUid())) {
            author = getResources().getString(R.string.you);
        } else {
            User user = RealmHelper.getInstance().getUser(selectedMessage.getFromId());
            author = user == null ? selectedMessage.getFromPhone() : user.getProperUserName();
        }

        showReplyLayout(author, selectedMessage);
        exitActionMode();
        KeyboardHelper.openSoftKeyboard(this, etMessage.findFocus());
        etMessage.requestFocus();

        currentQuotedMessage = selectedMessage;
    }


    private void clearChat() {
        DeleteDialog deleteDialog = new DeleteDialog(this, true);
        deleteDialog.setMTitle(getResources().getString(R.string.confirmation));
        deleteDialog.setMessage(R.string.clear_chat_message);
        deleteDialog.setmListener(new DeleteDialog.OnFragmentInteractionListener() {
            @Override
            public void onPositiveClick(boolean isDeleteChecked) {
                ProgressDialog progressDialog = new ProgressDialog(ChatActivity.this);
                progressDialog.setMessage(getResources().getString(R.string.deleting));
                RealmHelper.getInstance().clearChat(receiverUid, isDeleteChecked);
            }
        });


        deleteDialog.show();
    }


    //add a contact to Phonebook
    private void addToContacts() {
        Intent addContactIntent = IntentUtils.getAddContactIntent(user.getPhone());
        startActivity(addContactIntent);
    }

    private void blockUserClicked() {
        if (NetworkHelper.isConnected(this)) {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle(getString(R.string.loading));
            progressDialog.show();

            //unblock user
            Boolean setBlocked = !user.isBlocked();
            getDisposables().add(fireManager.setUserBlocked(fireManager.getUid(), receiverUid, setBlocked).subscribe(() -> {
                        progressDialog.dismiss();
                        RealmHelper.getInstance().setUserBlocked(user, setBlocked);
                        updateBlockMenuItemTitle(currentMenu);
                    }, throwable -> progressDialog.dismiss())
            );

        } else {
            Snackbar.make(findViewById(android.R.id.content), R.string.no_internet_connection, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void viewContactItemClicked() {
        viewContact();
    }

    private void searchItemClicked() {
        if (isInActionMode)
            exitActionMode();

        isInSearchMode = true;
        toolbar.getMenu().clear();
        searchGroup.setVisibility(View.VISIBLE);
        hideOrShowUserInfo(true);
        if (searchViewToolbar.isIconified())
            searchViewToolbar.onActionViewExpanded();

        searchViewToolbar.requestFocus();
    }

    private void copyItemClicked() {
        List<Message> selectedItemsForActionMode = viewModel.getSelectedItems();

        //sorting messages by timestamp
        //if the user selected the messages in a Random way
        Collections.sort(selectedItemsForActionMode);

        StringBuilder builder = new StringBuilder();
        for (Message message : selectedItemsForActionMode) {
            builder.append(message.getContent() + "\n");
        }

        String copiedString = builder.toString();
        ClipboardUtil.copyTextToClipboard(this, copiedString);
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
        exitActionMode();
    }

    private void forwardClicked() {
        Intent intent = new Intent(this, ForwardActivity.class);
        startActivityForResult(intent, FORWARD_MESSAGE_REQUEST);
    }

    private void shareClicked() {
        Message message = viewModel.getSelectedItems().get(0);
        if (message.getLocalPath() == null) return;
        Intent shareImageIntent = IntentUtils.getShareImageIntent(message.getLocalPath());
        shareImageIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        shareImageIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(shareImageIntent);
    }

    private void deleteItemClicked() {

        final List<Message> selectedItemsForActionMode = viewModel.getSelectedItems();
        boolean canDeleteForEveryOne = AdapterHelper.canDeleteForEveryOne(selectedItemsForActionMode);

        boolean containMedia = viewModel.isSelectedItemsContainMedia();

        if (canDeleteForEveryOne) {
            DeleteDialog deleteDialog = new DeleteDialog(this, containMedia, true);
            deleteDialog.setOnItemClick(new DeleteDialog.OnItemClick() {
                @Override
                public void onClick(int pos, boolean isDeleteChecked) {
                    switch (pos) {
                        //delete for me clicked
                        case 0:
                            for (Message message : selectedItemsForActionMode) {
                                if (message.getDownloadUploadStat() == LOADING) {
                                    if (MessageType.isSentType(message.getType())) {
                                        DownloadManager.cancelUpload(message.getMessageId());
                                    } else
                                        DownloadManager.cancelDownload(message.getMessageId());
                                }


                                RealmHelper.getInstance().deleteMessageFromRealm(message.getChatId(), message.getMessageId(), isDeleteChecked);

                            }
                            viewModel.clearSelectedItems();
                            exitActionMode();
                            break;


                        //delete for every one
                        case 2:
                            for (final Message message : selectedItemsForActionMode) {
                                getDisposables().add(fireManager.getServerTime().subscribe(timestamp -> {
                                            if (TimeHelper.isMessageTimePassed(timestamp, Long.parseLong(message.getTimestamp()))) {
                                                Toast.makeText(ChatActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                                            } else {
                                                FireConstants.getDeleteMessageRequestsRef(message.getMessageId(), user.isGroupBool(), user.isBroadcastBool(), user.getUid()).setValue(true).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        if (message.getDownloadUploadStat() == LOADING) {
                                                            if (MessageType.isSentType(message.getType())) {
                                                                DownloadManager.cancelUpload(message.getMessageId());
                                                            } else
                                                                DownloadManager.cancelDownload(message.getMessageId());
                                                        }
                                                        RealmHelper.getInstance().setMessageDeleted(message.getMessageId());
                                                    }
                                                });
                                            }
                                        }, error -> Toast.makeText(ChatActivity.this, R.string.error, Toast.LENGTH_SHORT).show())
                                );

                            }
                            exitActionMode();

                            break;
                    }
                }
            });

            deleteDialog.show();

        } else {

            DeleteDialog deleteDialog = new DeleteDialog(this, containMedia);
            deleteDialog.setmListener(new DeleteDialog.OnFragmentInteractionListener() {
                @Override
                public void onPositiveClick(boolean isDeleteChecked) {

                    for (Message message : selectedItemsForActionMode) {
                        if (message.getDownloadUploadStat() == LOADING) {
                            if (MessageType.isSentType(message.getType())) {
                                DownloadManager.cancelUpload(message.getMessageId());
                            } else
                                DownloadManager.cancelDownload(message.getMessageId());
                        }

                        RealmHelper.getInstance().deleteMessageFromRealm(message.getChatId(), message.getMessageId(), isDeleteChecked);
                    }


                    exitActionMode();

                }
            });
            deleteDialog.show();
        }


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_GALLERY_REQUEST && resultCode == RESULT_OK) {
            List<String> mPaths = Matisse.obtainPathResult(data);
            for (String mPath : mPaths) {
                if (!FileUtils.isFileExists(mPath)) {
                    Toast.makeText(ChatActivity.this, R.string.image_video_not_found, Toast.LENGTH_SHORT).show();
                    return;
                }

            }

            //Check if it's a video
            if (FileUtils.isPickedVideo(mPaths.get(0))) {

                sendTheVideo(mPaths);

            } else {
                sendImage(mPaths);
            }
        } else if (requestCode == PICK_MUSIC_REQUEST && resultCode == RESULT_OK) {

            Uri uri = data.getData();

            String[] audioArray = RealPathUtil.getAudioPath(this, uri);
            if (audioArray == null)
                Toast.makeText(this, R.string.could_not_get_audio_file, Toast.LENGTH_SHORT).show();
            else
                sendAudio(audioArray[0], audioArray[1]);

        } else if (requestCode == CAMERA_REQUEST && resultCode != ResultCodes.CAMERA_ERROR_STATE) {

            if (resultCode == ResultCodes.IMAGE_CAPTURE_SUCCESS) {
                String path = data.getStringExtra(IntentUtils.EXTRA_PATH_RESULT);
                sendImage(path, true);

            } else if (resultCode == ResultCodes.VIDEO_RECORD_SUCCESS) {
                String path = data.getStringExtra(IntentUtils.EXTRA_PATH_RESULT);
                sendTheVideo(path);

            }

            //if user choose to forward image to other users
        } else if (requestCode == FORWARD_MESSAGE_REQUEST && resultCode == RESULT_OK) {
            List<User> selectedList = data.getParcelableArrayListExtra(IntentUtils.EXTRA_DATA_RESULT);
            Toast.makeText(this, R.string.sending_messages, Toast.LENGTH_SHORT).show();
            sendForwardedMessages(viewModel.getSelectedItems(), selectedList);
            exitActionMode();

        } else if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK) {
            //get selected contacts from Phonebook
            List<ContactResult> results = MultiContactPicker.obtainResult(data);
            //convert results to expandableList so the user can choose which numbers he wants to send
            List<ExpandableContact> contactNameList = ContactUtils.getContactsFromContactResult(results);

            Intent intent = new Intent(this, SelectContactNumbersActivity.class);
            intent.putParcelableArrayListExtra(IntentUtils.EXTRA_CONTACT_LIST, (ArrayList<? extends Parcelable>) contactNameList);
            startActivityForResult(intent, PICK_NUMBERS_FOR_CONTACT_REQUEST);


        } else if (requestCode == PICK_NUMBERS_FOR_CONTACT_REQUEST && resultCode == RESULT_OK) {
            //get contacts after the user selects the numbers he wants to send
            List<ExpandableContact> selectedContacts = data.getParcelableArrayListExtra(IntentUtils.EXTRA_CONTACT_LIST);
            sendContacts(selectedContacts);
        } else if (requestCode == PICK_LOCATION_REQUEST && resultCode == RESULT_OK) {
            Place place = data.getParcelableExtra(Place.EXTRA_PLACE);
            sendLocation(place);
        }


    }


    private void sendLocation(Place place) {
        Message message = new MessageCreator.Builder(user, MessageType.SENT_LOCATION).quotedMessage(getQuotedMessage()).place(place).build();
        ServiceHelper.startNetworkRequest(this, message.getMessageId(), message.getChatId());

        //calling updateChat because the 'RealmChangeListener' may not be alive since the user launched another activity
        updateChat(message);
        hideReplyLayout();
    }

    private void sendContacts(List<ExpandableContact> selectedContacts) {
        List<Message> messages = new MessageCreator.Builder(user, MessageType.SENT_CONTACT).quotedMessage(getQuotedMessage()).contacts(selectedContacts).buildContacts();
        hideReplyLayout();

        for (Message message : messages) {
            ServiceHelper.startNetworkRequest(this, message.getMessageId(), message.getChatId());

            //calling updateChat because the 'RealmChangeListener' may not be alive since the user launched another activity
            updateChat(message);
        }

    }


    private void sendVoiceMessage(String path, String duration) {
        Message message = new MessageCreator.Builder(user, MessageType.SENT_VOICE_MESSAGE).quotedMessage(getQuotedMessage()).path(path).duration(duration).build();
        //addVoiceMessageStatListener to indicates when the recipient listened to this VoiceMessage
        addVoiceMessageStatListener(message.getMessageId());
        ServiceHelper.startNetworkRequest(this, message.getMessageId(), message.getChatId());
        hideReplyLayout();

    }


    private void sendTheVideo(String path) {
        Message message = new MessageCreator.Builder(user, MessageType.SENT_VIDEO).quotedMessage(getQuotedMessage()).path(path).context(this).build();
        ServiceHelper.startNetworkRequest(this, message.getMessageId(), message.getChatId());
        //calling updateChat because the 'RealmChangeListener' may not be alive since the user launched another activity
        updateChat(message);
        hideReplyLayout();

    }

    private void sendTheVideo(List<String> pathList) {
        for (String path : pathList) {
            Message message = new MessageCreator.Builder(user, MessageType.SENT_VIDEO).quotedMessage(getQuotedMessage()).path(path).context(this).build();
            ServiceHelper.startNetworkRequest(this, message.getMessageId(), message.getChatId());

            //calling updateChat because the 'RealmChangeListener' may not be alive since the user launched another activity
            updateChat(message);
        }
        hideReplyLayout();


    }

    private void sendMessage(Message message) {
        Message quotedMessage = getQuotedMessage();
        if (quotedMessage != null)
            message.setQuotedMessage(QuotedMessage.messageToQuotedMessage(quotedMessage));
        RealmHelper.getInstance().saveObjectToRealm(message);
        RealmHelper.getInstance().saveChatIfNotExists(message, user);
        ServiceHelper.startNetworkRequest(this, message.getMessageId(), message.getChatId());
        hideReplyLayout();

    }

    //send text message
    private void sendMessage(String text) {

        if (text.trim().isEmpty())
            return;

        int length = text.getBytes().length;
        if (length > FireConstants.MAX_SIZE_STRING) {
            Toast.makeText(ChatActivity.this, R.string.message_is_too_long, Toast.LENGTH_SHORT).show();
            return;
        }

        emojiPopup.dismiss();

        Message message = new MessageCreator.Builder(user, MessageType.SENT_TEXT).quotedMessage(getQuotedMessage()).text(text).build();
        ServiceHelper.startNetworkRequest(this, message.getMessageId(), message.getChatId());
        etMessage.setText("");
        hideReplyLayout();
    }

    //"isFromCamera" is when taking a picture ,because taking a picture from camera will save it directly in the app folder
    //send only one image
    private void sendImage(String filePath, boolean isFromCamera) {
        Message message = new MessageCreator.Builder(user, MessageType.SENT_IMAGE).quotedMessage(getQuotedMessage()).path(filePath).fromCamera(isFromCamera).build();
        ServiceHelper.startNetworkRequest(this, message.getMessageId(), message.getChatId());
        updateChat(message);
        hideReplyLayout();
    }


    //send multiple images
    private void sendImage(List<String> pathList) {
        for (String imagePath : pathList) {
            Message message = new MessageCreator.Builder(user, MessageType.SENT_IMAGE).quotedMessage(getQuotedMessage()).path(imagePath).fromCamera(false).build();
            ServiceHelper.startNetworkRequest(this, message.getMessageId(), message.getChatId());
            updateChat(message);
        }
        hideReplyLayout();

    }


    private void sendFile(final String filePath) {
        Message message = new MessageCreator.Builder(user, MessageType.SENT_FILE).quotedMessage(getQuotedMessage()).path(filePath).build();
        ServiceHelper.startNetworkRequest(this, message.getMessageId(), message.getChatId());
        hideReplyLayout();
    }

    private void sendAudio(final String filePath, String audioDuration) {
        Message message = new MessageCreator.Builder(user, MessageType.SENT_AUDIO).quotedMessage(getQuotedMessage()).path(filePath).duration(audioDuration).build();

        if (message == null) {
            Toast.makeText(this, R.string.space_or_permissions_error_toast, Toast.LENGTH_SHORT).show();
        } else {
            ServiceHelper.startNetworkRequest(this, message.getMessageId(), message.getChatId());
            updateChat(message);
        }
        hideReplyLayout();

    }

    private void sendForwardedMessages(List<Message> selectedMessages, List<User> selectedUsers) {
        for (User selectedUser : selectedUsers) {
            for (Message selectedMessage : selectedMessages) {
                Message message = MessageCreator.createForwardedMessage(selectedMessage, selectedUser, FireManager.getUid());
                ServiceHelper.startNetworkRequest(this, message.getMessageId(), message.getChatId());
            }
        }

        Toast.makeText(this, R.string.sending_messages, Toast.LENGTH_SHORT).show();

    }


    //this is called from adapter when the user is clicked on "X" Button
    //to cancel  upload or download process
    public void cancelDownloadOrUpload(Message message) {
        if (MessageType.isSentType(message.getType()))
            DownloadManager.cancelUpload(message);
        else
            DownloadManager.cancelDownload(message);
    }


    //this is called from adapter when user cancelled
    //the download process and want to re-download it
    public void download(Message message) {
        RealmHelper.getInstance().changeDownloadOrUploadStat(message.getMessageId(), LOADING);
        ServiceHelper.startNetworkRequest(this, message.getMessageId(), message.getChatId());
    }

    //this is called from adapter when user cancelled
    //the upload process and want to re-upload it
    public void upload(Message message) {
        RealmHelper.getInstance().updateDownloadUploadStat(message.getMessageId(), LOADING);
        ServiceHelper.startNetworkRequest(this, message.getMessageId(), message.getChatId());
    }


    private void loadMessagesList() {
        messageList = RealmHelper.getInstance().getMessagesInChat(receiverUid);
        observableList = RealmHelper.getInstance().getObservableList(receiverUid);
    }


    //this is called when the user attached or de-attached Headphones to the Device
    //therefore we want to stop listening for Earpiece sensor in Audio Service when it's attached
    @Subscribe
    public void headsetStateChanged(final HeadsetStateChanged state) {
        currentHeadsetState = state.getState();
        headsetStateChanged();
    }

    private void headsetStateChanged() {
        if (currentHeadsetState != -1)
            ServiceHelper.headsetStateChanged(this, currentHeadsetState);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void groupActiveStateChanged(GroupActiveStateChanged event) {
        if (!event.getGroupId().equals(user.getUid()))
            return;

        messageSwipeController.setGroupActive(event.isActive());

        hideOrShowTypingLayout(event.isActive());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateGroupEvent(UpdateGroupEvent event) {
        String groupId = event.getGroupId();
        if (groupId.equals(user.getUid())) {
            String groupTitle = user.getUserName();
            userNameToolbarChatActivity.setText(groupTitle);
            //check for new image and load it
            getUserPhoto();
            Group group = user.getGroup();
            if (group == null)return;
            if (group.isActive()) {
                hideOrShowTypingLayout(true);
            } else {
                hideOrShowTypingLayout(false);
                tvCantSendMessages.setText(R.string.you_cant_send_messages_to_group);
            }

            if (group.isOnlyAdminsCanPost()) {
                if (!fireManager.isAdmin(group.getAdminsUids())) {
                    hideOrShowTypingLayout(false);
                    tvCantSendMessages.setText(R.string.only_admins_can_post);
                } else {
                    hideOrShowTypingLayout(true);
                }
            }

            setMembersInToolbar();
            updateGroupCallButtonsVisibility(toolbar.getMenu());

        }
    }

    //get index from list using the id
    private int getPosFromId(String messageId) {
        Message message = new Message();
        message.setMessageId(messageId);
        return messageList.indexOf(message);
    }

    //when users selects a message
    public void onActionModeStarted() {
        //exit search and remove search from toolbar
        // if isInSearchMode
        if (isInSearchMode)
            exitSearchMode();

        //if it's not in action mode before
        //remove old menu items from toolbar
        //inflate action items and hide userInfo
        if (!isInActionMode) {
            toolbar.getMenu().clear();
            toolbar.inflateMenu(R.menu.menu_action_chat);
            hideOrShowUserInfo(true);
        }


        isInActionMode = true;

        //set items selected count as visible
        tvCounterAction.setVisibility(View.VISIBLE);
    }


    public void updateActionModeItemsCount(int itemsCount) {
        tvCounterAction.setText(itemsCount + "");
    }


    //hide or show the views in toolbar, userImg,userName,typing and available
    private void hideOrShowUserInfo(boolean hide) {
        int visibility = hide ? View.GONE : View.VISIBLE;
        userImgToolbarChatAct.setVisibility(visibility);
        userNameToolbarChatActivity.setVisibility(visibility);
        tvTypingStatToolbar.setVisibility(visibility);
        availableStatToolbar.setVisibility(visibility);
    }

    //when user click the back button
    @Override
    public void onBackPressed() {
        if (isInActionMode)
            exitActionMode();
        else if (isInSearchMode)
            exitSearchMode();
        else if (attachmentView.isShowing()) {
            attachmentView.hide(imgAttachment);
        } else
            super.onBackPressed();
    }

    public void exitActionMode() {
        adapter.notifyDataSetChanged();
        isInActionMode = false;
        tvCounterAction.setVisibility(View.GONE);
        toolbar.getMenu().clear();
        //re inflate default menu
        toolbar.inflateMenu(R.menu.menu_chat);
        invalidateOptionsMenu();
        hideOrShowUserInfo(false);
        //update online and typing tvs visibility after exiting action mode
        updateToolbarTvsVisibility(currentTypingState != TypingStat.NOT_TYPING);
        viewModel.clearSelectedItems();
    }

    private void exitSearchMode() {
        isInSearchMode = false;
        searchViewToolbar.onActionViewCollapsed();
        searchGroup.setVisibility(View.GONE);
        toolbar.inflateMenu(R.menu.menu_chat);
        hideOrShowUserInfo(false);
        //update online and typing tvs visibility after exiting search mode
        updateToolbarTvsVisibility(currentTypingState != TypingStat.NOT_TYPING);
        adapter.notifyDataSetChanged();
    }

    public void hideForwardItem() {
        MenuItem menuItem = toolbar.getMenu().findItem(R.id.menu_item_forward);
        if (menuItem != null)
            toolbar.getMenu().findItem(R.id.menu_item_forward).setVisible(false);
    }

    public void showForwardItem() {
        MenuItem menuItem = toolbar.getMenu().findItem(R.id.menu_item_forward);
        if (menuItem != null)
            toolbar.getMenu().findItem(R.id.menu_item_forward).setVisible(true);
    }


    //this is called from adapter when the user clicks on Play Button on Voice/Audio Message
    public void playAudio(final String id, final String url, final int pos,
                          final int progress) {

        //start audio service and play audio
        ServiceHelper.playAudio(this, id, url, pos, progress);


        final Message message = messageList.get(pos);


        //update the received voice message when the user listens to it
        if (message != null && !message.isGroup() && message.isVoiceMessage()
                && !message.getFromId().equals(FireManager.getUid())
                && !message.isVoiceMessageSeen()) { //check if it's already listened or not
            ServiceHelper.startUpdateVoiceMessageStatRequest(this, message.getMessageId(), message.getChatId(), fireManager.getUid());
        }


    }


    //update the network process progress
    @Subscribe
    public void onNetworkProgress(UpdateNetworkProgress event) {
        String messageId = event.getId();
        int progress = event.getProgress();
        viewModel.networkProgressChanged(messageId, progress);
    }


    //on finish network job
    @Subscribe
    public void onNetworkJobComplete(OnNetworkComplete data) {
        String messageId = data.getId();
        viewModel.removeNetworkProgress(messageId);


    }


    //this is for setting the max duration for the audio message
    //to make seekbar works properly
    @Subscribe
    public void onSetMax(AudioServiceCallbacksEvent.setMax event) {
        String id = event.getId();
        int duration = event.getDuration();


        viewModel.setAudibleMax(id, duration);


    }


    //when starts play
    @Subscribe
    public void onAudioPlay(AudioServiceCallbacksEvent.onPlay event) {
        String id = event.getId();
        int pos = event.getPos();
        int streamType = event.getStreamType();


        //set volume control when the user tries to press volume buttons
        //if it's in earpiece mode the volume control to earpiece
        setVolumeControlStream(streamType);


//        setViewHolderDrawableState(pos, true);

        viewModel.setAudiblePlayState(id, true);

        oldPosAudioPlayer = pos;
        oldIdAudioPlayer = id;


    }


    @Subscribe
    public void onAudioPause(AudioServiceCallbacksEvent.onPause event) {
        String id = event.getId();
        int pos = event.getPos();


        //set the volume buttons control back to default
        setVolumeControlStream(AudioManager.STREAM_SYSTEM);

        viewModel.setAudiblePlayState(id, false);


    }

    //when audio finishes
    @Subscribe
    public void onAudioComplete(AudioServiceCallbacksEvent.onComplete event) {
        String id = event.getId();
        int finalProgress = event.getFinalProgress();

        //set the volume buttons control back to default
        setVolumeControlStream(AudioManager.STREAM_MUSIC);


        viewModel.setAudibleComplete(id, finalProgress);

    }


    @Subscribe
    public void onAudioProgressUpdate(AudioServiceCallbacksEvent.onProgressUpdate event) {
        String id = event.getId();
        int pos = event.getPos();
        int progress = event.getProgress();
        byte[] waves = event.getWaves();


        viewModel.setAudibleProgress(id, progress, waves);


    }

    @Subscribe
    public void onAudioError(AudioServiceCallbacksEvent.onError event) {
        Toast.makeText(this, R.string.error_playing_this, Toast.LENGTH_SHORT).show();
    }


    public void seekTo(String id, int progress) {
        ServiceHelper.seekTo(this, id, progress);
    }


    public void onFileClick(Message message) {
        try {
            startActivity(IntentUtils.getOpenFileIntent(this, message.getLocalPath()));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.cannot_open_this_file, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void onContactBtnMessageClick(RealmContact contact) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.checking));
        RealmList<PhoneNumber> realmList = contact.getRealmList();
        //if contact has more than 1 number show a choose number dialog
        if (realmList.size() > 1) {
            DialogChoseNumber dialog = new DialogChoseNumber(this, realmList);

            dialog.setOnItemClickListener(new DialogChoseNumber.OnItemClickListener() {
                @Override
                public void onClick(String number) {
                    progressDialog.show();

                    //check if the contact has this app
                    Disposable disposable = fireManager.fetchAndSaveUserByPhone(number).subscribe(user -> {
                        progressDialog.dismiss();
                        if (user == null) {
                            SnackbarUtil.showDoesNotFireAppSnackbar(ChatActivity.this);
                        } else {
                            if (!user.getUid().equals(receiverUid) && !user.getUid().equals(FireManager.getUid())) {
                                startChatActivityWithDifferentUser(user);
                            }
                        }

                    }, throwable -> {
                        progressDialog.dismiss();
                        SnackbarUtil.showDoesNotFireAppSnackbar(ChatActivity.this);
                    }, () -> {
                        progressDialog.dismiss();
                        SnackbarUtil.showDoesNotFireAppSnackbar(ChatActivity.this);
                    });

                    //cancel it when clicking outside of dialog
                    progressDialog.setOnDismissListener(dialog1 -> {
                        disposable.dispose();
                    });

                    getDisposables().add(disposable);

                }
            });
            dialog.show();
            //only one number in contact
        } else {
            progressDialog.show();
            String number = realmList.get(0).getNumber();
            Disposable disposable = fireManager.fetchAndSaveUserByPhone(number).subscribe(user -> {
                progressDialog.dismiss();
                if (user == null) {
                    SnackbarUtil.showDoesNotFireAppSnackbar(ChatActivity.this);
                } else {
                    if (!user.getUid().equals(receiverUid) && !user.getUid().equals(FireManager.getUid())) {
                        startChatActivityWithDifferentUser(user);
                    }
                }

            }, throwable -> {
                progressDialog.dismiss();
                SnackbarUtil.showDoesNotFireAppSnackbar(ChatActivity.this);
            }, () -> {
                progressDialog.dismiss();
                SnackbarUtil.showDoesNotFireAppSnackbar(ChatActivity.this);
            });

            //cancel it when clicking outside of dialog
            progressDialog.setOnDismissListener(dialog1 -> {
                disposable.dispose();
            });

            getDisposables().add(disposable);
        }


    }


    private void startChatActivityWithDifferentUser(User user) {
        Intent intent = new Intent(ChatActivity.this, ChatActivity.class);
        intent.putExtra(IntentUtils.UID, user.getUid());
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public void showCopyItem() {
        MenuItem menuItem = toolbar.getMenu().findItem(R.id.menu_item_copy);
        if (menuItem != null)
            toolbar.getMenu().findItem(R.id.menu_item_copy).setVisible(true);
    }

    public void hideCopyItem() {
        MenuItem menuItem = toolbar.getMenu().findItem(R.id.menu_item_copy);
        if (menuItem != null)
            toolbar.getMenu().findItem(R.id.menu_item_copy).setVisible(false);
    }


    private void startUpdatePresenceTask() {
        if (!isGroup && !isBroadcast)
            updatePresenceHandler.postDelayed(updatePresenceRunnable, UPDATE_PRESENCE_DELAY);
    }

    private void stopUpdatePresenceTask() {
        updatePresenceHandler.removeCallbacks(updatePresenceRunnable);
    }


    private Runnable updatePresenceRunnable = new Runnable() {
        @Override
        public void run() {
            //if presence currentTypingState is not online then set last seen and update it every 2 Minutes
            if (!presenceStat.equals("Online")) {
                if (presenceTimestamp != 0) {
                    presenceStat = TimeHelper.getTimeAgo(presenceTimestamp);

                    availableStatToolbar.setText(presenceStat);
                    updateToolbarTvsVisibility(false);
                }
                updatePresenceHandler.postDelayed(this, UPDATE_PRESENCE_DELAY);
            }
        }
    };


    public void showShareItem() {
        MenuItem menuItem = toolbar.getMenu().findItem(R.id.menu_item_share);
        if (menuItem != null)
            toolbar.getMenu().findItem(R.id.menu_item_share).setVisible(true);
    }


    public void hideShareItem() {
        MenuItem menuItem = toolbar.getMenu().findItem(R.id.menu_item_share);
        if (menuItem != null)
            toolbar.getMenu().findItem(R.id.menu_item_share).setVisible(false);
    }


    private void showBlockedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
        builder.setTitle(R.string.user_is_blocked)
                .setPositiveButton(R.string.unblock, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        blockUserClicked();
                    }
                }).setNegativeButton(R.string.cancel, null)
                .show();
    }

    private int getLastVisibileItem() {
        return linearLayoutManager.findLastVisibleItemPosition();
    }


    //update network processes progress
    private void updateProgressData() {
        for (ProgressData progressData : DownloadManager.progressDataHashMap.values()) {
            String messageId = progressData.getMessageId();
            int progress = progressData.getProgress();
            onNetworkProgress(new UpdateNetworkProgress(messageId, progress));
        }
    }


    //this is called when a user in group starts typing
    @Override
    public void onTyping(int state, String groupId, User user) {
        if (user == null) return;
        String name = user.getProperUserName();
        availableStatToolbar.setText(name + " is " + TypingStat.getStatString(ChatActivity.this, state));


    }

    //this is called when there is no one is typing in a group
    @Override
    public void onAllNotTyping(String groupId) {
        setMembersInToolbar();
    }

    private void showReplyLayout(String messageAuthor, Message message) {
        btnCancelImage.setVisibility(View.VISIBLE);
        animateReplyLayout(true);


        tvQuotedName.setText(messageAuthor);
        tvQuotedText.setText(MessageTypeHelper.getMessageContent(message, false));
        if (message.getThumb() != null) {
            quotedThumb.setVisibility(View.VISIBLE);
            Glide.with(this).load(message.getThumb()).into(quotedThumb);
        } else
            quotedThumb.setVisibility(View.GONE);

        if (!message.isTextMessage() && MessageTypeHelper.getMessageTypeDrawable(message.getType()) != -1) {
            int messageTypeResource = MessageTypeHelper.getMessageTypeDrawable(message.getType());
            if (messageTypeResource != -1) {
                Drawable drawable = getResources()
                        .getDrawable(messageTypeResource);
                drawable.mutate().setColorFilter(ContextCompat.getColor(this, R.color.grey), PorterDuff.Mode.SRC_IN);
                tvQuotedText.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
            }
        } else
            tvQuotedText.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);

        tvQuotedText.setTextColor(ContextCompat.getColor(this, R.color.colorText));

    }

    private void animateReplyLayout(final boolean showLayout) {
        if (showLayout)
            quotedMessageFrame.setVisibility(View.VISIBLE);
        else {
            if (quotedMessageFrame.getVisibility() == View.GONE)//don't animate if it's already hidden
                return;
        }


        float fromY = showLayout ? typingLayout.getBottom() : typingLayout.getTop();
        float toY = showLayout ? typingLayout.getTop() : typingLayout.getBottom();


        Animation
                animation = new TranslateAnimation(0, 0, fromY, toY);
        animation.setDuration(250);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) quotedMessageFrame.getLayoutParams();
                quotedMessageFrame.setLayoutParams(params);
                if (!showLayout)
                    quotedMessageFrame.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        quotedMessageFrame.startAnimation(animation);
    }

    public void hideReplyMenuItem() {
        MenuItem menuItem = toolbar.getMenu().findItem(R.id.menu_item_reply);
        if (menuItem != null)
            toolbar.getMenu().findItem(R.id.menu_item_reply).setVisible(false);
    }

    public void showReplyItem() {
        MenuItem menuItem = toolbar.getMenu().findItem(R.id.menu_item_reply);
        if (menuItem != null)
            toolbar.getMenu().findItem(R.id.menu_item_reply).setVisible(true);
    }

    private Message getQuotedMessage() {
        if (quotedMessageFrame.getVisibility() == View.GONE)
            return null;


        return currentQuotedMessage;
    }

    private void hideReplyLayout() {
        //quotedMessageFrame.setVisibility(View.GONE);
        animateReplyLayout(false);
        currentQuotedMessage = null;
    }

    public void highlightQuotedMessage(QuotedMessage message) {

        int index = Message.getPosFromId(message.getMessageId(), messageList);
        if (index != -1) {
            scrollAndHighlightQuotedMessage(index);
        }
    }

    private void animateQuotedMessageBg(View view) {
        stopAnimation();
        if (colorAnim != null)//use only single instance
            colorAnim = null;

        colorAnim = ObjectAnimator.ofInt(view, "backgroundColor", getResources().getColor(R.color.item_selected_background_color), Color.TRANSPARENT);
        colorAnim.setDuration(2500);
        colorAnim.setEvaluator(new ArgbEvaluator());
        colorAnim.start();
    }

    public void stopAnimation() {
        if (colorAnim != null) {
            colorAnim.cancel();
        }
    }


    @Override
    public void onContainerViewClick(int pos, @NotNull View view, @NotNull Message message) {
        if (isInActionMode) {
            viewModel.itemSelected(pos, message);
        } else {
            switch (message.getType()) {
                case MessageType.SENT_IMAGE:
                case MessageType.RECEIVED_IMAGE:
                    if (message.getType() == MessageType.SENT_IMAGE ||
                            message.getDownloadUploadStat() == DownloadUploadStat.SUCCESS) {
                        String path = message.getLocalPath();
                        String messageId = message.getMessageId();

                        if (!FileUtils.isFileExists(path)) {
                            Toast.makeText(ChatActivity.this, R.string.item_deleted_from_storage, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Intent intent = new Intent(ChatActivity.this, FullscreenActivity.class);
                        intent.putExtra(IntentUtils.EXTRA_PATH, path);
                        intent.putExtra(IntentUtils.UID, user.getUid());
                        intent.putExtra(IntentUtils.EXTRA_MESSAGE_ID, messageId);
                        intent.putExtra(IntentUtils.EXTRA_STARTING_POSITION, pos);

                        int firstVisibleItemPosition = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
                        int lastVisibleItemPosition = linearLayoutManager.findLastCompletelyVisibleItemPosition();

                        intent.putExtra(IntentUtils.EXTRA_FIRST_VISIBLE_ITEM_POSITION, firstVisibleItemPosition);
                        intent.putExtra(IntentUtils.EXTRA_LAST_VISIBLE_ITEM_POSITION, lastVisibleItemPosition);


                        if (!mIsDetailsActivityStarted) {
                            mIsDetailsActivityStarted = true;

                            ImageView imageView = view.findViewById(R.id.img_msg);
                            if (imageView != null && ViewCompat.getTransitionName(imageView) != null)
                                startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(ChatActivity.this,
                                        imageView, ViewCompat.getTransitionName(imageView)).toBundle());

                        }
                    }
                    break;

                case MessageType.RECEIVED_VIDEO:
                case MessageType.SENT_VIDEO:
                    if (message.getType() == MessageType.SENT_VIDEO ||
                            message.getDownloadUploadStat() == DownloadUploadStat.SUCCESS) {

                        String path = message.getLocalPath();
                        String messageId = message.getMessageId();

                        if (!FileUtils.isFileExists(path)) {
                            Toast.makeText(ChatActivity.this, R.string.item_deleted_from_storage, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Intent intent = new Intent(ChatActivity.this, FullscreenActivity.class);
                        intent.putExtra(IntentUtils.EXTRA_PATH, path);
                        intent.putExtra(IntentUtils.UID, user.getUid());
                        intent.putExtra(IntentUtils.EXTRA_MESSAGE_ID, messageId);
                        intent.putExtra(IntentUtils.EXTRA_STARTING_POSITION, pos);

                        int firstVisibleItemPosition = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
                        int lastVisibleItemPosition = linearLayoutManager.findLastCompletelyVisibleItemPosition();

                        intent.putExtra(IntentUtils.EXTRA_FIRST_VISIBLE_ITEM_POSITION, firstVisibleItemPosition);
                        intent.putExtra(IntentUtils.EXTRA_LAST_VISIBLE_ITEM_POSITION, lastVisibleItemPosition);


                        if (!mIsDetailsActivityStarted) {
                            mIsDetailsActivityStarted = true;


                            startActivity(intent);


                        }
                    }
                    break;

                case MessageType.SENT_LOCATION:
                case MessageType.RECEIVED_LOCATION:
                    Intent openMapIntent = IntentUtils.getOpenMapIntent(message.getLocation());
                    if (openMapIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(openMapIntent);
                    }
                    break;

                case MessageType.RECEIVED_FILE:
                case MessageType.SENT_FILE:
                    if (message.getDownloadUploadStat() == DownloadUploadStat.SUCCESS)
                        onFileClick(message);

                    break;

                case MessageType.SENT_CONTACT:
                case MessageType.RECEIVED_CONTACT:
                    Intent intent = new Intent(this, ContactDetailsActivity.class);
                    intent.putExtra(IntentUtils.EXTRA_MESSAGE_ID, message.getMessageId());
                    intent.putExtra(IntentUtils.EXTRA_CHAT_ID, message.getChatId());
                    startActivity(intent);
                    break;

                case MessageType.RECEIVED_AUDIO:
                case MessageType.SENT_AUDIO:
                case MessageType.RECEIVED_VOICE_MESSAGE:
                case MessageType.SENT_VOICE_MESSAGE:
                    int progress = viewModel.getAudibleProgressForId(message.getMessageId()) == -1 ? 0 : viewModel.getAudibleProgressForId(message.getMessageId());
                    playAudio(message.getMessageId(), message.getLocalPath(), pos, progress);
                    break;


            }
        }
    }

    @Override
    public void onItemViewClick(int pos, @NotNull View itemView, @NotNull Message message) {
        if (isInActionMode)
            viewModel.itemSelected(pos, message);

    }

    @Override
    public void onLongClick(int pos, @NotNull View itemView, @NotNull Message message) {
        if (!isInActionMode) {
            onActionModeStarted();
            viewModel.itemSelected(pos, message);
        }
    }

    @Override
    public void onProgressButtonClick(int pos, @NotNull View itemView, @NotNull Message message) {

        if (isInActionMode) {
            viewModel.itemSelected(pos, message);
        } else {
            if (message.getDownloadUploadStat() == LOADING) {
                cancelDownloadOrUpload(message);
            } else if (message.getDownloadUploadStat() == CANCELLED || message.getDownloadUploadStat() == FAILED) {

                if (MessageType.isSentType(message.getType())) {
                    upload(message);
                } else {
                    download(message);
                }

            }
        }

    }

    @Override
    public void onQuotedMessageClick(int pos, @NotNull View itemView, @NotNull Message message) {
        QuotedMessage quotedMessage = message.getQuotedMessage();
        if (quotedMessage.getStatus() != null) {
            Status status = RealmHelper.getInstance().getStatus(quotedMessage.getStatus().getStatusId());
            if (status != null) {
                Intent intent = new Intent(this, ViewStatusActivity.class);
                intent.putExtra(IntentUtils.UID, status.getUserId());
                startActivity(intent);
            }
        } else {
            highlightQuotedMessage(quotedMessage);
        }
    }

    //Message Click for Contacts
    @Override
    public void onMessageClick(@NotNull RealmContact contact) {
        if (isInActionMode) return;

        onContactBtnMessageClick(contact);
    }


    @Override
    public void onAddContactClick(@NotNull RealmContact contact) {
        if (isInActionMode) return;


        Intent addContactIntent = IntentUtils.getAddContactIntent(contact);
        startActivity(addContactIntent);

    }

    //OnSeek for Audible Messages
    @Override
    public void onSeek(@NotNull Message message, int progress, int seekBarMax) {
        if (isInActionMode) return;

        //if user changed the seekbar while it's not playing
        String messageId = message.getMessageId();

        if (seekBarMax == 100) {
            int max = (int) Util.getMediaLengthInMillis(this, message.getLocalPath());
            if (max == 0) return; //if file not found or missing permissions

            int realProgress = max / 100 * progress;
            viewModel.setAudibleMax(messageId, max);
            viewModel.setAudibleProgress(messageId, realProgress, null);


        }
        viewModel.setAudibleProgress(messageId, progress, null);


        seekTo(messageId, progress);

    }
}







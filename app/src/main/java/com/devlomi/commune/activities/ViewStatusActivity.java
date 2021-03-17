package com.devlomi.commune.activities;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.cjt2325.cameralibrary.ResultCodes;
import com.codekidlabs.storagechooser.StorageChooser;
import com.devlomi.commune.R;
import com.devlomi.commune.adapters.StatusSeenByAdapter;
import com.devlomi.commune.adapters.StatusSeenByCallback;
import com.devlomi.commune.model.ExpandableContact;
import com.devlomi.commune.model.constants.MessageType;
import com.devlomi.commune.model.constants.StatusType;
import com.devlomi.commune.model.constants.TypingStat;
import com.devlomi.commune.model.realms.Message;
import com.devlomi.commune.model.realms.QuotedMessage;
import com.devlomi.commune.model.realms.Status;
import com.devlomi.commune.model.realms.StatusSeenBy;
import com.devlomi.commune.model.realms.TextStatus;
import com.devlomi.commune.model.realms.User;
import com.devlomi.commune.placespicker.Place;
import com.devlomi.commune.placespicker.PlacesPickerActivity;
import com.devlomi.commune.utils.BitmapUtils;
import com.devlomi.commune.utils.ContactUtils;
import com.devlomi.commune.utils.DirManager;
import com.devlomi.commune.utils.DpUtil;
import com.devlomi.commune.utils.FileFilter;
import com.devlomi.commune.utils.FileUtils;
import com.devlomi.commune.utils.FireConstants;
import com.devlomi.commune.utils.IntentUtils;
import com.devlomi.commune.utils.KeyboardHelper;
import com.devlomi.commune.utils.KeyboardUtils;
import com.devlomi.commune.utils.MessageCreator;
import com.devlomi.commune.utils.MyApp;
import com.devlomi.commune.utils.RealPathUtil;
import com.devlomi.commune.utils.RealmHelper;
import com.devlomi.commune.utils.RecorderSettings;
import com.devlomi.commune.utils.ServiceHelper;
import com.devlomi.commune.utils.SharedPreferencesManager;
import com.devlomi.commune.utils.StatusHelper;
import com.devlomi.commune.utils.TimeHelper;
import com.devlomi.commune.utils.Util;
import com.devlomi.commune.utils.network.FireManager;
import com.devlomi.commune.utils.network.StatusManager;
import com.devlomi.commune.views.AnimButton;
import com.devlomi.commune.views.AttachmentView;
import com.devlomi.record_view.OnBasketAnimationEnd;
import com.devlomi.record_view.OnRecordClickListener;
import com.devlomi.record_view.OnRecordListener;
import com.devlomi.record_view.RecordView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.vanniktech.emoji.EmojiEditText;
import com.vanniktech.emoji.EmojiPopup;
import com.vanniktech.emoji.EmojiTextView;
import com.wafflecopter.multicontactpicker.ContactResult;
import com.wafflecopter.multicontactpicker.MultiContactPicker;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import io.realm.RealmResults;
import jp.shts.android.storiesprogressview.StoriesProgressView;
import me.zhanghai.android.systemuihelper.SystemUiHelper;
import omrecorder.AudioChunk;
import omrecorder.OmRecorder;
import omrecorder.PullTransport;
import omrecorder.Recorder;
import ooo.oxo.library.widget.PullBackLayout;

import static com.devlomi.commune.activities.main.messaging.ChatActivity.CAMERA_REQUEST;
import static com.devlomi.commune.activities.main.messaging.ChatActivity.MAX_FILE_SIZE;
import static com.devlomi.commune.activities.main.messaging.ChatActivity.PICK_CONTACT_REQUEST;
import static com.devlomi.commune.activities.main.messaging.ChatActivity.PICK_GALLERY_REQUEST;
import static com.devlomi.commune.activities.main.messaging.ChatActivity.PICK_LOCATION_REQUEST;
import static com.devlomi.commune.activities.main.messaging.ChatActivity.PICK_MUSIC_REQUEST;
import static com.devlomi.commune.activities.main.messaging.ChatActivity.PICK_NUMBERS_FOR_CONTACT_REQUEST;
import static com.devlomi.commune.activities.main.messaging.ChatActivity.RECORD_START_AUDIO_LENGTH;
import static com.devlomi.commune.utils.FontUtil.isFontExists;

public class ViewStatusActivity extends BaseActivity implements StoriesProgressView.StoriesListener, PullBackLayout.Callback, StatusSeenByCallback {


    String userId;
    StoriesProgressView storiesProgressView;
    private ImageView image;

    private VideoView videoView;
    private CircleImageView profileImage;
    private TextView tvUsername;
    private TextView tvStatusTime;
    private ImageButton backButton;
    private ConstraintLayout root;
    private TextView tvStatus;
    private RecyclerView rvSeenBy;
    private AttachmentView attachmentView;
    MediaPlayer.OnPreparedListener onPreparedListener;
    MediaPlayer.OnErrorListener onErrorListener;
    private ProgressBar progressBar;
    private StatusSeenByAdapter adapter;
    private int counter = 0;
    private TextView tvSeenCount;
    private ImageView arrowUp, replyArrowUp;


    private LinearLayout typingLayout;
    private ImageView emojiBtn;
    private EmojiEditText etMessage;
    private EmojiPopup emojiPopup;
    private ImageView imgAttachment;
    private ImageView cameraBtn;

    private RecordView recordView;
    private AnimButton recordButton;
    String timerStr = "";


    private LinearLayout bottomSheetSeen, bottomSheetReply;
    SystemUiHelper systemUiHelper;
    //image story duration 7 seconds
    public static final long IMAGE_STORY_DURATION = 7000L;
    //text story duration 6 seconds
    private static final long TEXT_STORY_DURATION = 6000L;

    long pressTime = 0L;
    long limit = 500L;
    RealmResults<Status> statuses;

    private FrameLayout quotedMessageFrame;
    private View quotedColor;
    private EmojiTextView tvQuotedName;
    private EmojiTextView tvQuotedText;
    private ImageView quotedThumb;
    private ImageView btnCancelImage;
    private View replyDimView;


    User user;

    Recorder recorder;
    File recordFile;
    BottomSheetBehavior seenByBottomsheetBehavior, replyBehavior;
    RealmResults<StatusSeenBy> statusSeenBy;
    private StatusManager statusManager = new StatusManager();

    //on touch listener, when user holds his thumb it will pause the story,and when he release it will resume
    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (isSeenByExpanded() || isReplyExpanded()) {
                        return false;
                    }

                    pause();
                    return false;
                case MotionEvent.ACTION_UP:
                    return resume();
            }

            return false;
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        systemUiHelper = new SystemUiHelper(this, SystemUiHelper.LEVEL_HIDE_STATUS_BAR, SystemUiHelper.FLAG_IMMERSIVE_STICKY);
        systemUiHelper.hide();

        setContentView(R.layout.activity_view_status);


        userId = getIntent().getStringExtra(IntentUtils.UID);
        statuses = RealmHelper.getInstance().getUserStatuses(userId).getFilteredStatuses();

        if (userId.equals(FireManager.getUid()))
            user = SharedPreferencesManager.getCurrentUser();
        else
            user = RealmHelper.getInstance().getUser(userId);

        initViews();
        quotedMessageFrame.setVisibility(View.VISIBLE);
        setQuotedMessageStyle();
        //set stories durations
        storiesProgressView.setStoriesCountWithDurations(getDurations());
        storiesProgressView.setStoriesListener(this);


        //onVideo prepared listener
        onPreparedListener = new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                image.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                storiesProgressView.start(counter);
                videoView.start();
            }
        };

        onErrorListener = new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                Toast.makeText(ViewStatusActivity.this, R.string.error_playing_this, Toast.LENGTH_SHORT).show();
                return true;
            }
        };

        if (statuses.size() >= 0) {


            loadStatus(statuses.get(0));
            storiesProgressView.startStories(counter);
        }

        // bind reverse view
        //play the previous story (onClick)
        View reverse = findViewById(R.id.reverse);
        reverse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storiesProgressView.reverse(counter);
            }
        });

        reverse.setOnTouchListener(onTouchListener);

        // bind skip view
        //play the next story (onClick)
        View skip = findViewById(R.id.skip);
        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storiesProgressView.skip(counter);
            }
        });
        skip.setOnTouchListener(onTouchListener);


        //back button in toolbar onClick
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });


        PullBackLayout pullBackLayout = findViewById(R.id.pull);
        pullBackLayout.setCallback(this);

        setUserInfo(user);


        seenByBottomsheetBehavior = BottomSheetBehavior.from(bottomSheetSeen);

        seenByBottomsheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_EXPANDED:


                        rvSeenBy.animate().translationY(-DpUtil.toPixel(50, MyApp.context())).start();
                        arrowUp.animate().rotation(180).setDuration(200).start();
                        pause();
                        break;

                    case BottomSheetBehavior.STATE_COLLAPSED:
                        arrowUp.animate().rotation(0).setDuration(200).start();
                        rvSeenBy.animate().translationY(0).start();
                        resume();

                        break;


                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });


        replyBehavior = BottomSheetBehavior.from(bottomSheetReply);

        replyBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    KeyboardHelper.hideSoftKeyboard(ViewStatusActivity.this, bottomSheet);
                    replyArrowUp.animate().rotation(0).setDuration(200).start();
                    replyDimView.setVisibility(View.GONE);

                    resume();
                } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    replyArrowUp.animate().rotation(180).setDuration(200).start();
                    replyDimView.setVisibility(View.VISIBLE);
                    pause();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });


        showReplyLayout();

        imgAttachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                attachmentView.reveal(view);
                KeyboardHelper.hideSoftKeyboard(ViewStatusActivity.this, etMessage);


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

        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startCamera();
            }
        });

        emojiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emojiPopup.toggle();
            }
        });

        recordView.setCancelBounds(0);
        recordView.setSlideToCancelArrowColor(ContextCompat.getColor(this,R.color.iconTintColor));
        recordView.setCounterTimeColor(ContextCompat.getColor(this,R.color.colorText));
        recordView.setSlideToCancelTextColor(ContextCompat.getColor(this,R.color.colorText));
        recordButton.setRecordView(recordView);

        recordButton.setOnRecordClickListener(new OnRecordClickListener() {
            @Override
            public void onClick(View v) {
                String text = etMessage.getText().toString();
                sendMessage(text);
            }
        });
        recordView.setOnRecordListener(new OnRecordListener() {
            @Override
            public void onStart() {
                hideOrShowRecord(false);
                getDisposables().add(getFireManager().setTypingStat(user.getUid(), TypingStat.RECORDING, false, false).subscribe());
                handleRecord();
            }

            @Override
            public void onCancel() {
                stopRecord(true, -1);
                getDisposables().add(getFireManager().setTypingStat(userId, TypingStat.NOT_TYPING, false, false).subscribe());
            }

            @Override
            public void onFinish(long recordTime) {
                hideOrShowRecord(true);

                getDisposables().add(getFireManager().setTypingStat(userId, TypingStat.NOT_TYPING, false, false).subscribe());
                stopRecord(false, recordTime);
                requestEditTextFocus();
            }

            @Override
            public void onLessThanSecond() {
                Toast.makeText(ViewStatusActivity.this, R.string.voice_message_is_short_toast, Toast.LENGTH_SHORT).show();
                hideOrShowRecord(true);
                getDisposables().add(getFireManager().setTypingStat(userId, TypingStat.NOT_TYPING, false, false).subscribe());
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

        imgAttachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                attachmentView.reveal(view);
                KeyboardHelper.hideSoftKeyboard(ViewStatusActivity.this, etMessage);


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


                } else if (text.trim().length() == 0) {
                    changeSendButtonState(false);
                }
            }
        });


        if (user.getUid() == FireManager.getUid())
            bottomSheetSeen.setVisibility(View.VISIBLE);
        else
            bottomSheetReply.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        KeyboardUtils.addKeyboardToggleListener(this, isVisible -> {
            systemUiHelper.hide();
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        KeyboardUtils.removeAllKeyboardToggleListeners();
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

            //otherwise get the recordTime and convert it to Readable String and send the message
            timerStr = Util.milliSecondsToTimer(recordTime);
            String filePath = recordFile.getPath();
            sendVoiceMessage(filePath, timerStr);
        }

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
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                etMessage.requestFocus();
            }
        }, 100);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_GALLERY_REQUEST && resultCode == RESULT_OK) {
            List<String> mPaths = Matisse.obtainPathResult(data);
            for (String mPath : mPaths) {
                if (!FileUtils.isFileExists(mPath)) {
                    Toast.makeText(ViewStatusActivity.this, R.string.image_video_not_found, Toast.LENGTH_SHORT).show();
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
        sendFinished();

    }

    private void sendContacts(List<ExpandableContact> selectedContacts) {
        List<Message> messages = new MessageCreator.Builder(user, MessageType.SENT_CONTACT).quotedMessage(getQuotedMessage()).contacts(selectedContacts).buildContacts();


        for (Message message : messages) {
            ServiceHelper.startNetworkRequest(this, message.getMessageId(), message.getChatId());

            //calling updateChat because the 'RealmChangeListener' may not be alive since the user launched another activity
            sendFinished();

        }

    }


    private void sendVoiceMessage(String path, String duration) {
        Message message = new MessageCreator.Builder(user, MessageType.SENT_VOICE_MESSAGE).quotedMessage(getQuotedMessage()).path(path).duration(duration).build();
        //addVoiceMessageStatListener to indicates when the recipient listened to this VoiceMessage
        ServiceHelper.startNetworkRequest(this, message.getMessageId(), message.getChatId());

        sendFinished();
    }


    private void sendTheVideo(String path) {
        Message message = new MessageCreator.Builder(user, MessageType.SENT_VIDEO).quotedMessage(getQuotedMessage()).path(path).context(this).build();
        ServiceHelper.startNetworkRequest(this, message.getMessageId(), message.getChatId());

        //calling updateChat because the 'RealmChangeListener' may not be alive since the user launched another activity
        sendFinished();


    }

    private void sendTheVideo(List<String> pathList) {
        for (String path : pathList) {
            Message message = new MessageCreator.Builder(user, MessageType.SENT_VIDEO).quotedMessage(getQuotedMessage()).path(path).context(this).build();
            ServiceHelper.startNetworkRequest(this, message.getMessageId(), message.getChatId());

            //calling updateChat because the 'RealmChangeListener' may not be alive since the user launched another activity
            sendFinished();

        }


    }

    private void sendMessage(Message message) {
        Message quotedMessage = getQuotedMessage();
        if (quotedMessage != null)
            message.setQuotedMessage(QuotedMessage.messageToQuotedMessage(quotedMessage));
        RealmHelper.getInstance().saveObjectToRealm(message);
        RealmHelper.getInstance().saveChatIfNotExists(message, user);
        ServiceHelper.startNetworkRequest(this, message.getMessageId(), message.getChatId());


    }

    private Message getQuotedMessage() {
        if (!isCurrentIndexValid(counter)) return null;

        Status status = statuses.get(counter);
        return Status.statusToMessage(status, userId);
    }

    //send text message
    private void sendMessage(String text) {

        if (text.trim().isEmpty())
            return;

        int length = text.getBytes().length;
        if (length > FireConstants.MAX_SIZE_STRING) {
            Toast.makeText(ViewStatusActivity.this, R.string.message_is_too_long, Toast.LENGTH_SHORT).show();
            return;
        }

        emojiPopup.dismiss();

        Message message = new MessageCreator.Builder(user, MessageType.SENT_TEXT).quotedMessage(getQuotedMessage()).text(text).build();

        ServiceHelper.startNetworkRequest(this, message.getMessageId(), message.getChatId());
        etMessage.setText("");

        sendFinished();
    }

    //"isFromCamera" is when taking a picture ,because taking a picture from camera will save it directly in the app folder
    //send only one image
    private void sendImage(String filePath, boolean isFromCamera) {
        Message message = new MessageCreator.Builder(user, MessageType.SENT_IMAGE).quotedMessage(getQuotedMessage()).path(filePath).fromCamera(isFromCamera).build();
        ServiceHelper.startNetworkRequest(this, message.getMessageId(), message.getChatId());

        sendFinished();

    }


    //send multiple images
    private void sendImage(List<String> pathList) {
        for (String imagePath : pathList) {
            Message message = new MessageCreator.Builder(user, MessageType.SENT_IMAGE).quotedMessage(getQuotedMessage()).path(imagePath).fromCamera(false).build();
            ServiceHelper.startNetworkRequest(this, message.getMessageId(), message.getChatId());

        }

        sendFinished();

    }


    private void sendFile(final String filePath) {
        Message message = new MessageCreator.Builder(user, MessageType.SENT_FILE).quotedMessage(getQuotedMessage()).path(filePath).build();
        ServiceHelper.startNetworkRequest(this, message.getMessageId(), message.getChatId());

        sendFinished();

    }

    private void sendAudio(final String filePath, String audioDuration) {
        Message message = new MessageCreator.Builder(user, MessageType.SENT_AUDIO).quotedMessage(getQuotedMessage()).path(filePath).duration(audioDuration).build();

        if (message == null) {
            Toast.makeText(this, R.string.space_or_permissions_error_toast, Toast.LENGTH_SHORT).show();
        } else {
            ServiceHelper.startNetworkRequest(this, message.getMessageId(), message.getChatId());
            sendFinished();

        }


    }

    private void sendFinished() {
        Toast.makeText(ViewStatusActivity.this, R.string.sending_reply, Toast.LENGTH_SHORT).show();
        KeyboardHelper.hideSoftKeyboard(this, etMessage);
        replyBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void startCamera() {
        startActivityForResult(new Intent(ViewStatusActivity.this, CameraActivity.class), CAMERA_REQUEST);
    }

    private void pickImages() {
        Matisse.from(ViewStatusActivity.this)
                .choose(MimeType.of(MimeType.MP4, MimeType.THREEGPP, MimeType.THREEGPP2
                        , MimeType.JPEG, MimeType.BMP, MimeType.PNG, MimeType.GIF))
                .countable(true)
                .maxSelectable(1)
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
        new MultiContactPicker.Builder(ViewStatusActivity.this)
                .handleColor(ContextCompat.getColor(ViewStatusActivity.this, R.color.colorPrimary))
                .bubbleColor(ContextCompat.getColor(ViewStatusActivity.this, R.color.colorPrimary))
                .showPickerForResult(PICK_CONTACT_REQUEST);
    }

    private void pickFile() {
        StorageChooser chooser = new StorageChooser.Builder()
                .withActivity(ViewStatusActivity.this)
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
                    Toast.makeText(ViewStatusActivity.this, R.string.file_is_too_big, Toast.LENGTH_SHORT).show();

                } else if (!FileFilter.isOkExtension(fileExtension)) {
                    Toast.makeText(ViewStatusActivity.this, R.string.type_not_supported, Toast.LENGTH_SHORT).show();
                } else {
                    sendFile(path);
                }

            }
        });


    }

    private void setupAdapter(Status status) {
        adapter = new StatusSeenByAdapter(statusSeenBy, this);
        rvSeenBy.setLayoutManager(new LinearLayoutManager(this));
        rvSeenBy.setAdapter(adapter);


        getDisposables().add(statusManager.getStatusSeenByList(status.getStatusId()).subscribe(pair -> {
            Status currentStatus = statuses.get(counter);
            String statusId = pair.component1();
            List<StatusSeenBy> statusSeenBy = pair.component2();
            if (statusId.equals(currentStatus.getStatusId()))
                tvSeenCount.setText(statusSeenBy.size() + "");
        }, throwable -> {

        }));
    }


    private void showReplyLayout() {
        if (!isCurrentIndexValid(counter)) return;

        btnCancelImage.setVisibility(View.VISIBLE);

        Status status = statuses.get(counter);


        tvQuotedName.setText(user.getProperUserName());
        tvQuotedText.setText(StatusHelper.INSTANCE.getStatusContent(status));
        if (status.getThumbImg() != null) {
            quotedThumb.setVisibility(View.VISIBLE);
            Glide.with(this).load(status.getThumbImg()).into(quotedThumb);
        } else
            quotedThumb.setVisibility(View.GONE);

        if (status.getType() != StatusType.TEXT && StatusHelper.INSTANCE.getStatusTypeDrawable(status.getType()) != -1) {
            int messageTypeResource = StatusHelper.INSTANCE.getStatusTypeDrawable(status.getType());
            if (messageTypeResource != -1) {
                Drawable drawable = getResources()
                        .getDrawable(messageTypeResource);
                drawable.mutate().setColorFilter(ContextCompat.getColor(this, R.color.grey), PorterDuff.Mode.SRC_IN);
                tvQuotedText.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
            }
        } else
            tvQuotedText.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
    }

    private void setQuotedMessageStyle() {
        quotedMessageFrame.setBackgroundColor(ContextCompat.getColor(this, R.color.quoted_sent_background_color));
        tvQuotedName.setTextColor(ContextCompat.getColor(this, R.color.quoted_sent_text_color));
        quotedColor.setBackgroundColor(ContextCompat.getColor(this, R.color.quoted_sent_quoted_color));
        btnCancelImage.setColorFilter(ContextCompat.getColor(this, R.color.quoted_cancel_color), PorterDuff.Mode.SRC_IN);
    }

    @Override
    protected void onDestroy() {
        videoView.stopPlayback();
        videoView.setOnPreparedListener(null);
        videoView.setOnErrorListener(null);
        storiesProgressView.destroy();
        super.onDestroy();
    }

    private boolean resume() {
        long now = System.currentTimeMillis();

        //resume video if needed
        if (statuses.get(counter).getType() == StatusType.VIDEO && !videoView.isPlaying()) {
            videoView.start();
        }
        storiesProgressView.resume();
        return limit < now - pressTime;
    }


    private void pause() {
        pressTime = System.currentTimeMillis();
        storiesProgressView.pause();
        //pause video if needed
        if (statuses.get(counter).getType() == StatusType.VIDEO && videoView.isPlaying()) {
            videoView.pause();
        }
    }


    private void initViews() {
        storiesProgressView = findViewById(R.id.stories);
        image = findViewById(R.id.image);
        videoView = findViewById(R.id.video_view);
        profileImage = findViewById(R.id.profile_image);
        tvUsername = findViewById(R.id.tv_username);
        tvStatusTime = findViewById(R.id.tv_status_time);
        backButton = findViewById(R.id.back_button);
        progressBar = findViewById(R.id.progress_bar);
        tvStatus = findViewById(R.id.tv_status);
        tvSeenCount = findViewById(R.id.tv_seen_count);
        arrowUp = findViewById(R.id.arrow_up);
        rvSeenBy = findViewById(R.id.rv_seen_by);
        bottomSheetSeen = findViewById(R.id.bottom_sheet_seen_by);
        bottomSheetReply = findViewById(R.id.bottom_sheet_reply_status);
        quotedMessageFrame = findViewById(R.id.quoted_message_frame);
        quotedColor = findViewById(R.id.quoted_color);
        tvQuotedName = findViewById(R.id.tv_quoted_name);
        tvQuotedText = findViewById(R.id.tv_quoted_text);
        quotedThumb = findViewById(R.id.quoted_thumb);
        btnCancelImage = findViewById(R.id.btn_cancel_image);
        replyDimView = findViewById(R.id.reply_dim_view);

        typingLayout = findViewById(R.id.typing_layout);
        emojiBtn = findViewById(R.id.emoji_btn);
        etMessage = findViewById(R.id.et_message);
        imgAttachment = findViewById(R.id.img_attachment);
        cameraBtn = findViewById(R.id.camera_btn);

        root = findViewById(R.id.root);
        recordView = findViewById(R.id.record_view);
        recordButton = findViewById(R.id.record_button);

        attachmentView = findViewById(R.id.attachment_view);

        storiesProgressView.setStoriesCount(statuses.size());

        replyArrowUp = findViewById(R.id.reply_arrow_up);

        emojiPopup = EmojiPopup.Builder.fromRootView(root)
                .setOnEmojiPopupShownListener(() -> emojiBtn.setImageResource(R.drawable.ic_baseline_keyboard_24))
                .setOnEmojiPopupDismissListener(() -> emojiBtn.setImageResource(R.drawable.ic_insert_emoticon_black))
                .build(etMessage);


    }

    @Override
    public void onNext() {
        videoView.stopPlayback();
        videoView.setOnPreparedListener(null);
        videoView.setOnErrorListener(null);
        int newCounter = counter + 1;

        if (newCounter >= 0 && newCounter < statuses.size()) {
            counter = newCounter;
            loadStatus(statuses.get(counter));
        } else {
            return;
        }
    }

    private boolean isCurrentIndexValid(int index) {

        if (index >= 0 && index < statuses.size()) {
            return true;
        }
        return false;
    }

    @Override
    public void onPrev() {

        videoView.stopPlayback();
        videoView.setOnPreparedListener(null);
        videoView.setOnErrorListener(null);


        int newCounter = counter - 1;

        if (newCounter >= 0 && newCounter < statuses.size()) {
            counter = newCounter;
            loadStatus(statuses.get(counter));
        } else {
            return;
        }

    }

    @Override
    public void onComplete() {
        finish();
    }

    private void loadStatus(final Status status) {

        setStatusTime(status.getTimestamp());
        storiesProgressView.setCurrent(counter);
        videoView.setVisibility(View.GONE);
        image.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        image.setVisibility(View.VISIBLE);
        tvStatus.setVisibility(View.GONE);
        root.setBackgroundColor(Color.BLACK);

        //load thumb blurred image while loading original image or video
        if (status.getType() == StatusType.IMAGE || status.getType() == StatusType.VIDEO)
            image.setImageBitmap(BitmapUtils.simpleBlur(this, BitmapUtils.encodeImage(status.getThumbImg())));


        if (status.getType() == StatusType.IMAGE) {
            loadImage(status);
        } else if (status.getType() == StatusType.VIDEO) {
            loadVideo(status);
        } else if (status.getType() == StatusType.TEXT) {
            progressBar.setVisibility(View.GONE);
            tvStatus.setVisibility(View.VISIBLE);
            videoView.setVisibility(View.GONE);
            image.setVisibility(View.GONE);
            loadTextStatus(status.getTextStatus());
        }

        statusSeenBy = RealmHelper.getInstance().getSeenByList(status.getSeenBy());
        tvSeenCount.setText(statusSeenBy.size() + "");
        setupAdapter(status);

        //set status as seen
        if (!status.isSeen()) {
            RealmHelper.getInstance().setStatusAsSeen(status.getStatusId());
            //check if all statuses are seen and save it
            if (status.getStatusId().equals(statuses.last().getStatusId()))
                RealmHelper.getInstance().setAllStatusesAsSeen(userId);
        }
        //Schedule a job to update status count on Firebase
        if (!status.getUserId().equals(FireManager.getUid()) && !status.isSeenCountSent()) {
            getDisposables().add(statusManager.setStatusSeen(userId, status.getStatusId()).subscribe(() -> {

            }, throwable -> {

            }));
        }


    }

    private void loadTextStatus(TextStatus textStatus) {
        try {
            String color = textStatus.getBackgroundColor();
            root.setBackgroundColor(Color.parseColor(color));
            String fontName = textStatus.getFontName();

            if (isFontExists(fontName)) {
                tvStatus.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/" + fontName));
            }

            tvStatus.setText(textStatus.getText());
            storiesProgressView.start(counter);
        } catch (Exception e) {
            root.setBackgroundColor(Color.BLACK);

        }

    }


    private void loadImage(Status status) {

        //if this status by this user load it locally ,otherwise load it from server and cache it
        String url = status.getLocalPath() == null ? status.getContent() : status.getLocalPath();

        Glide.with(this).load(url).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                progressBar.setVisibility(View.GONE);
                storiesProgressView.start(counter);
                image.setImageDrawable(resource);
                return false;
            }
        }).into(image);
    }

    private void loadVideo(Status status) {
        //if the video is not exists download it
        if (status.getLocalPath() == null) {
            downloadStatusVideo(status);
        } else {
            //if the video is exists in device play it
            if (FileUtils.isFileExists(status.getLocalPath())) {
                playVideo(status.getLocalPath());
            } else {
                //otherwise download it
                downloadStatusVideo(status);
            }
        }
    }


    private void downloadStatusVideo(Status status) {

        File statusFile = DirManager.getReceivedStatusFile(status.getStatusId(), status.getType());
        getDisposables().add(statusManager.downloadVideoStatus(status.getStatusId(), status.getContent(), statusFile).subscribe(filePath -> {
            Status currentStatus = statuses.get(counter);

            if (isCurrentIndexValid(counter) && currentStatus != null &&
                    status.getStatusId().equals(currentStatus.getStatusId())
                    && filePath != null) {

                playVideo(filePath);

            }
        }, throwable -> {

        }));
    }

    private void playVideo(String path) {
        videoView.requestFocus();
        videoView.setVideoURI(Uri.parse(path));
        videoView.setVisibility(View.VISIBLE);
        videoView.setOnPreparedListener(onPreparedListener);
        videoView.setOnErrorListener(onErrorListener);
    }


    //get statuses durations
    private long[] getDurations() {
        long[] array = new long[statuses.size()];
        for (int i = 0; i < statuses.size(); i++) {
            Status status = statuses.get(i);
            //if it's an image set its duration to IMAGE_STORY_DURATION
            if (status.getType() == StatusType.IMAGE) {
                array[i] = IMAGE_STORY_DURATION;
            } else if (status.getType() == StatusType.TEXT) {
                array[i] = TEXT_STORY_DURATION;
            } else {
                //if it's a video set its duration to the video duration
                array[i] = status.getDuration();
            }
        }
        return array;
    }

    @Override
    public void onPullStart() {
        isFinishing = false;
    }

    float currentVelocity = 0;

    private boolean isReplyExpanded() {
        return replyBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED;
    }

    private boolean isSeenByExpanded() {
        return seenByBottomsheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED;
    }

    @Override
    public void onPull(float v) {
        currentVelocity = v;
        if (isFinishing
                || isSeenByExpanded()
                || isReplyExpanded())
            return;
        int height = root.getHeight();
        float newAlpha = 1 - v;
        float trans = height * v;
        if (v >= 0.2) {
            root.animate().translationY(height).setDuration(200).start();
            root.animate().alpha(0).setDuration(200).start();
            isFinishing = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, 200);
        } else {
            root.animate().alpha(newAlpha).setDuration(0).start();
            root.animate().translationY(trans).setDuration(0).start();
        }
    }

    boolean isFinishing = false;

    @Override
    public void onPullCancel() {
        if (currentVelocity <= 0.2) {

            root.animate().translationY(0).setDuration(200).start();
            root.animate().alpha(1).setDuration(200).start();
        }
        resume();
    }

    //when the user swipes vertically exit the activity
    @Override
    public void onPullComplete() {
//        finish();
    }

    //set status time
    private void setStatusTime(long timestamp) {
        tvStatusTime.setText(TimeHelper.getStatusTime(timestamp));
    }

    //set user image and user info
    private void setUserInfo(User user) {
        Glide.with(this).load(user.getThumbImg()).into(profileImage);
        if (user.getUid().equals(FireManager.getUid()))
            tvUsername.setText(getResources().getString(R.string.you));
        else
            tvUsername.setText(user.getProperUserName());
    }

    @Override
    public void onClick(@NotNull User user, @NotNull View itemView) {

    }

    @Override
    public void onBackPressed() {

        if (attachmentView.isShowing()) {
            attachmentView.hide(imgAttachment);
        } else
            super.onBackPressed();
    }

    private void changeSendButtonState(boolean setTyping) {
        if (setTyping) {
            recordButton.goToState(AnimButton.TYPING_STATE);
            recordButton.setListenForRecord(false);

        } else {
            recordButton.goToState(AnimButton.RECORDING_STATE);
            recordButton.setListenForRecord(true);
        }

    }

    @Override
    public boolean enablePresence() {
        return false;
    }
}

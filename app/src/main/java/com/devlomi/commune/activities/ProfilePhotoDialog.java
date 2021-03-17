package com.devlomi.commune.activities;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import com.bumptech.glide.Glide;
import com.devlomi.commune.R;
import com.devlomi.commune.activities.main.messaging.ChatActivity;
import com.devlomi.commune.model.realms.User;
import com.devlomi.commune.utils.FileUtils;
import com.devlomi.commune.utils.IntentUtils;
import com.devlomi.commune.utils.RealmHelper;
import com.devlomi.commune.utils.Util;


public class ProfilePhotoDialog extends BaseActivity {


    private ImageView imageViewUserProfileDialog;
    private TextView tvUsernameDialog;

    private ImageButton buttonInfoDialog;
    private ImageButton buttonMessageDialog;
    private User user;

    private boolean isBroadcast;


    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (!Util.isOreoOrAbove()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_profile_photo_dialog);
        initViews();


        String uid = getIntent().getStringExtra(IntentUtils.UID);
        user = RealmHelper.getInstance().getUser(uid);
        isBroadcast = user.isBroadcastBool();
        tvUsernameDialog.setText(user.getProperUserName());

        loadUserImg();


        //show the image in ProfilePhotoActivity
        imageViewUserProfileDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfilePhotoDialog.this, ProfilePhotoActivity.class);
                intent.putExtra(IntentUtils.UID, user.getUid());
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        //show the user info
        buttonInfoDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProfilePhotoDialog.this, UserDetailsActivity.class);
                intent.putExtra(IntentUtils.UID, user.getUid());
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        //start Chat with this user
        buttonMessageDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProfilePhotoDialog.this, ChatActivity.class);
                intent.putExtra(IntentUtils.UID, user.getUid());
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });


    }

    private void loadUserImg() {
        //check if image is exists in database and in storage
        //if it's available show it
        if (isBroadcast) {
            Drawable drawable = AppCompatResources.getDrawable(this, R.drawable.ic_broadcast_with_bg);
            imageViewUserProfileDialog.setImageDrawable(drawable);
        } else if (user.getUserLocalPhoto() != null && FileUtils.isFileExists(user.getUserLocalPhoto())) {
            Glide.with(this)
                    .load(user.getUserLocalPhoto())
                    .into(imageViewUserProfileDialog);

            //otherwise show thumbImg if it's exists
        } else if (user.getThumbImg() != null) {
//            byte[] bytes = BitmapUtils.encodeImageAsBytes(user.getThumbImg());
            Glide.with(this).load(user.getThumbImg()).into(imageViewUserProfileDialog);
        }
    }


    private void initViews() {
        imageViewUserProfileDialog = findViewById(R.id.image_view_user_profile_dialog);
        tvUsernameDialog = findViewById(R.id.tv_username_dialog);
        buttonInfoDialog = findViewById(R.id.button_info_dialog);
        buttonMessageDialog = findViewById(R.id.button_message_dialog);
    }

    @Override
    protected void onStop() {
        super.onStop();
    getDisposables().dispose();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //load user info once it's downloaded

        //check if there is a new image for this user
        //if yes ,download it and show it
        if (!isBroadcast)
            getDisposables().add(getFireManager().checkAndDownloadUserPhoto(user).subscribe(imageItem -> {
                if (imageItem == null) return;
                String image = null;
                if (imageItem.getPhoto() != null) {
                    image = imageItem.getPhoto();

                } else if (imageItem.getThumbImg() != null) {
                    image = imageItem.getThumbImg();
                }

                if (image != null)
                    Glide.with(ProfilePhotoDialog.this).load(image).into(imageViewUserProfileDialog);
            },throwable -> {

            }));


    }

    @Override
    public boolean enablePresence() {
        return false;
    }
}

package com.devlomi.commune.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.Toast;

import com.devlomi.commune.R;
import com.devlomi.commune.activities.main.messaging.ChatActivity;
import com.devlomi.commune.model.realms.User;
import com.devlomi.commune.utils.GroupLinkUtil;
import com.devlomi.commune.utils.IntentUtils;
import com.devlomi.commune.utils.RealmHelper;
import com.devlomi.commune.utils.Util;
import com.devlomi.commune.utils.network.FireManager;
import com.devlomi.commune.utils.network.GroupManager;
import com.devlomi.commune.views.AcceptInviteBottomSheet;

public class AcceptInviteLink extends BaseActivity implements AcceptInviteBottomSheet.BottomSheetCallbacks {
    String groupId;
    private AcceptInviteBottomSheet bottomSheet;
    private GroupManager groupManager = new GroupManager();


    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!Util.isOreoOrAbove()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();

        bottomSheet = new AcceptInviteBottomSheet();
        bottomSheet.show(getSupportFragmentManager(), "");


        if (intent.getData() == null || intent.getData().getLastPathSegment() == null) {
            onInvalidLink();
        } else {

            String groupLink = intent.getData().getLastPathSegment();
            //check if link is valid
            GroupLinkUtil.isGroupLinkValid(groupLink, new GroupLinkUtil.GetGroupByLinkCallback() {
                @Override
                public void onFound(final String groupId) {
                    AcceptInviteLink.this.groupId = groupId;
                    //if chat user already in group do nothing
                    User user = RealmHelper.getInstance().getUser(groupId);
                    if (user != null && user.getGroup() != null && user.getGroup().isActive()) {
                        alreadyInGroup();
                        return;
                    }

                    //check if user is banned from group

                    getDisposables().add(groupManager.isUserBannedFromGroup(groupId, FireManager.getUid()).subscribe(isBanned -> {
                        if (isBanned) {
                            Toast.makeText(AcceptInviteLink.this, R.string.banned_from_group, Toast.LENGTH_SHORT).show();
                            finish();

                        } else {
                            getDisposables().add(groupManager.fetchGroupPartialInfo(groupId).subscribe(pair -> {
                                User groupUser = pair.component1();
                                int groupUsersCount = pair.component2();

                                bottomSheet.showData(groupUser, groupUsersCount);
                            }, throwable -> {
                                Toast.makeText(AcceptInviteLink.this, R.string.unknown_error, Toast.LENGTH_SHORT).show();
                                finish();
                            }));
                        }
                    }, throwable -> {
                        Toast.makeText(AcceptInviteLink.this, R.string.unknown_error, Toast.LENGTH_SHORT).show();
                        finish();
                    }));

                }

                @Override
                public void onError() {
                    onInvalidLink();
                }
            });
        }


    }

    private void alreadyInGroup() {
        Toast.makeText(this, R.string.you_are_already_joined_the_group, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void onInvalidLink() {
        Toast.makeText(this, getString(R.string.invalid_group_link), Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onDismiss() {
        finish();
    }

    @Override
    public void onJoinBtnClick() {
        if (groupId == null) return;
        if (bottomSheet != null) {
            bottomSheet.showLoadingOnJoin();
        }
        getDisposables().add(groupManager.joinViaGroupLink(groupId).subscribe(() -> {
            Intent mIntent = new Intent(AcceptInviteLink.this, ChatActivity.class);
            mIntent.putExtra(IntentUtils.UID, groupId);
            startActivity(mIntent);
            finish();
        }, throwable -> {
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            finish();
        }));

    }

    @Override
    public boolean enablePresence() {
        return false;
    }
}

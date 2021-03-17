package com.devlomi.commune.views;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Group;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.devlomi.commune.R;
import com.devlomi.commune.adapters.PartialGroupUsersAdapter;
import com.devlomi.commune.model.realms.User;
import com.devlomi.commune.utils.ContactUtils;
import com.devlomi.commune.utils.MyApp;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class AcceptInviteBottomSheet extends BottomSheetDialogFragment {
    private TextView tvFetchingGroup;
    private Group cgroupFetching;
    private Button btnJoinGroup;
    private Button btnCancel;
    private RecyclerView recyclerView;
    private TextView tvParticipantsCount;
    private ImageView imgGroupIcon;
    private TextView tvGroupName;
    private TextView tvGroupCreator;
    private Group cgroupGroupInfo;

    BottomSheetCallbacks callbacks;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.accept_invite_bottom_sheet, container, false);
        initViews(view);
        tvFetchingGroup.setText(MyApp.context().getResources().getString(R.string.fetching_group));
        cgroupFetching.setVisibility(View.VISIBLE);
        cgroupGroupInfo.setVisibility(View.GONE);

        btnJoinGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (callbacks != null) callbacks.onJoinBtnClick();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });


        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        callbacks = (BottomSheetCallbacks) context;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (callbacks != null)
            callbacks.onDismiss();
    }

    private void initViews(View view) {
        tvFetchingGroup = view.findViewById(R.id.tv_fetching_group);
        cgroupFetching = view.findViewById(R.id.cgroup_fetching);
        btnJoinGroup = view.findViewById(R.id.btn_join_group);
        btnCancel = view.findViewById(R.id.btn_cancel);
        recyclerView = view.findViewById(R.id.rv);
        tvParticipantsCount = view.findViewById(R.id.tv_participants_count);
        imgGroupIcon = view.findViewById(R.id.img_group_icon);
        tvGroupName = view.findViewById(R.id.tv_group_name);
        tvGroupCreator = view.findViewById(R.id.tv_group_creator);
        cgroupGroupInfo = view.findViewById(R.id.cgroup_group_info);
    }

    public void showData(User user, int usersCount) {
        cgroupFetching.setVisibility(View.GONE);
        cgroupGroupInfo.setVisibility(View.VISIBLE);
        tvGroupName.setText(user.getProperUserName());
        com.devlomi.commune.model.realms.Group group = user.getGroup();
        if (group != null) {
            String groupCreator = MyApp.context().getResources().getString(R.string.created_by) + " " + ContactUtils.queryForNameByNumber( group.getCreatedByNumber());
            tvGroupCreator.setText(groupCreator);
            tvParticipantsCount.setText(usersCount + " " + MyApp.context().getResources().getString(R.string.participants));
            PartialGroupUsersAdapter adapter = new PartialGroupUsersAdapter(group.getUsers(), getActivity());
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
            recyclerView.setAdapter(adapter);
            try {
                Glide.with(getActivity()).load(user.getPhoto()).into(imgGroupIcon);
            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }

    public void showLoadingOnJoin() {
        cgroupGroupInfo.setVisibility(View.GONE);
        cgroupFetching.setVisibility(View.VISIBLE);
        tvFetchingGroup.setText(MyApp.context().getResources().getString(R.string.loading));

    }

    public interface BottomSheetCallbacks {
        void onDismiss();

        void onJoinBtnClick();
    }
}

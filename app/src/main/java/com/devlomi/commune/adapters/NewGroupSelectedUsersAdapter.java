package com.devlomi.commune.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.devlomi.commune.R;
import com.devlomi.commune.model.realms.User;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class NewGroupSelectedUsersAdapter extends RecyclerView.Adapter<NewGroupSelectedUsersAdapter.NewGroupSelectedUsersHolder> {
    private List<User> selecetedUsers;
    private Context context;
    OnUserClick onUserClick;

    public NewGroupSelectedUsersAdapter(List<User> selecetedUsers, Context context, OnUserClick onUserClick) {
        this.selecetedUsers = selecetedUsers;
        this.context = context;
        this.onUserClick = onUserClick;

    }

    @NonNull
    @Override
    public NewGroupSelectedUsersHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_new_group_selected_user, parent, false);
        return new NewGroupSelectedUsersHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull NewGroupSelectedUsersHolder holder, int position) {
        User user = selecetedUsers.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return selecetedUsers.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    class NewGroupSelectedUsersHolder extends RecyclerView.ViewHolder {
        private CircleImageView userImgSelectedUserGroup;
        private TextView tvSelectedUserGroup;
        private FrameLayout deleteSelectedUserGroupLayout;


        public NewGroupSelectedUsersHolder(View itemView) {
            super(itemView);
            userImgSelectedUserGroup = itemView.findViewById(R.id.user_img_selected_user_group);
            tvSelectedUserGroup = itemView.findViewById(R.id.tv_selected_user_group);
            deleteSelectedUserGroupLayout = itemView.findViewById(R.id.delete_selected_user_group_layout);
        }

        public void bind(final User user) {
            Glide.with(context).load(user.getThumbImg()).into(userImgSelectedUserGroup);
            tvSelectedUserGroup.setText(user.getProperUserName());

            deleteSelectedUserGroupLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (onUserClick != null)
                        onUserClick.onRemove(user);
                }
            });

        }
    }

    public interface OnUserClick {
        void onRemove(User user);
    }

}

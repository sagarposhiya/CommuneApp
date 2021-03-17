package com.devlomi.commune.fragments;


import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.devlomi.commune.R;
import com.devlomi.commune.interfaces.ToolbarStateChange;
import com.devlomi.commune.model.realms.Message;
import com.github.chrisbanes.photoview.PhotoView;


public class ImageViewFragment extends Fragment {

    private Context context;
    private Message message;
    private PhotoView photoView;
    ToolbarStateChange toolbarStateChange;


    private int mStartingPosition;
    private int mItemPosition;


    private static final String ARG_MEDIA_ITEM_POSITION = "arg_media_item_position";
    private static final String ARG_STARTING_MEDIA_ITEM_POSITION = "arg_starting_media_item_position";


    public ImageViewFragment() {
        // Required empty public constructor
    }

    public void setContext(Context context) {
        this.context = context;
        toolbarStateChange = (ToolbarStateChange) context;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStartingPosition = getArguments().getInt(ARG_STARTING_MEDIA_ITEM_POSITION);
        mItemPosition = getArguments().getInt(ARG_MEDIA_ITEM_POSITION);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_image_view, container, false);
        photoView = view.findViewById(R.id.photo_view);


        ViewCompat.setTransitionName(photoView, message.getMessageId());
        Glide.with(context).load(message.getLocalPath()).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                startPostponedTransition();
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                startPostponedTransition();
                return false;
            }
        }).into(photoView);


        //hide toolbar and system bars when click on image view
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toolbarStateChange.toggle();
            }
        });

        return view;
    }

    //transitions
    private void startPostponedTransition() {
        if (mItemPosition == mStartingPosition) {
            photoView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    photoView.getViewTreeObserver().removeOnPreDrawListener(this);
                    getActivity().supportStartPostponedEnterTransition();
                    return true;
                }
            });
        }
    }

    //get instance of ImageViewFragment
    public static ImageViewFragment create(Context context, Message message, int currentPosition, int mStartingPosition) {
        Bundle args = new Bundle();
        //pass arguments from constructor
        args.putInt(ARG_MEDIA_ITEM_POSITION, currentPosition);
        args.putInt(ARG_STARTING_MEDIA_ITEM_POSITION, mStartingPosition);
        ImageViewFragment fragment = new ImageViewFragment();

        fragment.setContext(context);
        fragment.setMessage(message);
        fragment.setArguments(args);

        return fragment;
    }


    public View getImageView() {
        if (isViewInBounds(getActivity().getWindow().getDecorView(), photoView)) {
            return photoView;
        }

        return null;
    }

    private static boolean isViewInBounds(@NonNull View container, @NonNull View view) {
        Rect containerBounds = new Rect();
        container.getHitRect(containerBounds);
        return view.getLocalVisibleRect(containerBounds);
    }

}


package com.devlomi.commune.activities.settings;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityOptionsCompat;
import androidx.preference.PreferenceFragmentCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.devlomi.commune.Base;
import com.devlomi.commune.R;
import com.devlomi.commune.activities.ProfilePhotoActivity;
import com.devlomi.commune.utils.BitmapUtils;
import com.devlomi.commune.utils.CropImageRequest;
import com.devlomi.commune.utils.DirManager;
import com.devlomi.commune.utils.IntentUtils;
import com.devlomi.commune.utils.NetworkHelper;
import com.devlomi.commune.utils.SharedPreferencesManager;
import com.devlomi.commune.utils.network.FireManager;
import com.theartofdev.edmodo.cropper.CropImage;

import org.jetbrains.annotations.NotNull;

import java.io.File;

import de.hdodenhof.circleimageview.CircleImageView;
import io.reactivex.disposables.CompositeDisposable;

import static android.app.Activity.RESULT_OK;

/**
 * Created by Devlomi on 25/03/2018.
 */

public class ProfilePreferenceFragment extends PreferenceFragmentCompat implements Base {
    public static final int PICK_IMAGE_REQUEST = 4951;
    private CircleImageView imageViewUserProfile;
    private ImageButton imageButtonChangeUserProfile;
    private ImageButton imageButtonEditUsername;
    private TextView tvUsername;
    private TextView tvStatus;
    private TextView tvPhoneNumber;

    private FireManager fireManager = new FireManager();
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void pickImages() {
        CropImageRequest.getCropImageRequest().start(getActivity(), this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fagment_profile_settings, container, false);

        imageViewUserProfile = view.findViewById(R.id.image_view_user_profile);
        imageButtonChangeUserProfile = view.findViewById(R.id.image_button_change_user_profile);
        tvUsername = view.findViewById(R.id.tv_username);
        imageButtonEditUsername = view.findViewById(R.id.image_button_edit_username);
        tvStatus = view.findViewById(R.id.tv_status);
        tvPhoneNumber = view.findViewById(R.id.tv_phone_number);


        String userName = SharedPreferencesManager.getUserName();
        String status = SharedPreferencesManager.getStatus();
        String phoneNumber = SharedPreferencesManager.getPhoneNumber();
        final String myPhoto = SharedPreferencesManager.getMyPhoto();
        tvStatus.setText(status);
        tvUsername.setText(userName);
        tvPhoneNumber.setText(phoneNumber);


        imageViewUserProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ProfilePhotoActivity.class);
                String transName = "profile_photo_trans";

                intent.putExtra(IntentUtils.EXTRA_PROFILE_PATH, myPhoto);
                startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), v, transName).toBundle());
            }
        });

        imageButtonChangeUserProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImages();
            }
        });

        imageButtonEditUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditTextDialog(getString(R.string.enter_your_name), new EditTextDialogListener() {
                    @Override
                    public void onOk(final String text) {
                        if (TextUtils.isEmpty(text)) {
                            Toast.makeText(getActivity(), R.string.username_is_empty, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (NetworkHelper.isConnected(getActivity())) {
                            getDisposables().add(
                                    fireManager.updateMyUserName(text).subscribe(() -> {
                                        SharedPreferencesManager.saveMyUsername(text);
                                        tvUsername.setText(text);
                                    }, throwable -> Toast.makeText(getActivity(), R.string.no_internet_connection, Toast.LENGTH_SHORT).show())
                            );

                        } else {
                            Toast.makeText(getActivity(), R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        tvStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditTextDialog(getString(R.string.enter_your_status), new EditTextDialogListener() {
                    @Override
                    public void onOk(final String text) {
                        if (TextUtils.isEmpty(text)) {
                            Toast.makeText(getActivity(), R.string.status_is_empty, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (NetworkHelper.isConnected(getActivity())) {
                            getDisposables().add(
                                    fireManager.updateMyStatus(text).subscribe(() -> {
                                        SharedPreferencesManager.saveMyStatus(text);
                                        tvStatus.setText(text);

                                    }, throwable -> Toast.makeText(getActivity(), R.string.no_internet_connection, Toast.LENGTH_SHORT).show())
                            );


                        } else {
                            Toast.makeText(getActivity(), R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        Glide.with(getActivity()).load(new File(myPhoto))
                .into(imageViewUserProfile);

        return view;

    }

    private void showEditTextDialog(String message, final EditTextDialogListener listener) {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        final EditText edittext = new EditText(getActivity());
        alert.setMessage(message);


        alert.setView(edittext);

        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {


                if (listener != null)
                    listener.onOk(edittext.getText().toString());


            }
        });

        alert.setNegativeButton(R.string.cancel, null);

        alert.show();


    }

    @NotNull
    @Override
    public CompositeDisposable getDisposables() {
        return disposables;
    }


    private interface EditTextDialogListener {
        void onOk(String text);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();


//                final File file = DirManager.getMyPhotoPath();
                final File file = DirManager.generateUserProfileImage();


                BitmapUtils.compressImage(resultUri.getPath(), file, 30);

                getDisposables().add(fireManager.updateMyPhoto(file.getPath()).subscribe(tiple -> {
                            Glide.with(getActivity())
                                    .load(file)
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .skipMemoryCache(true)
                                    .into(imageViewUserProfile);
                            Toast.makeText(getActivity(), R.string.image_changed, Toast.LENGTH_SHORT).show();
                        }, throwable -> {

                        })
                );


            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }
}


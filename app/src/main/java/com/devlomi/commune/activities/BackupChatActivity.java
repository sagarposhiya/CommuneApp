package com.devlomi.commune.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.devlomi.commune.R;
import com.devlomi.commune.utils.RealmBackupRestore;
import com.devlomi.commune.utils.SharedPreferencesManager;
import com.devlomi.commune.utils.TimeHelper;
import com.devlomi.commune.utils.Util;

import io.realm.internal.IOException;

public class BackupChatActivity extends AppCompatActivity {
    private TextView tvLastBackup;
    private Button btnBackup;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_chat);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.colorBlack));
        tvLastBackup = findViewById(R.id.tv_last_backup);
        btnBackup = findViewById(R.id.btn_backup);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setLastBackupTime();

        btnBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProgressDialog progressDialog = new ProgressDialog(BackupChatActivity.this);
                progressDialog.setTitle(R.string.backing_up);
                progressDialog.setMessage(getResources().getString(R.string.backing_up_message));
                progressDialog.show();

                try {
                    new RealmBackupRestore(BackupChatActivity.this).backup();
                    Util.showSnackbar(BackupChatActivity.this, getResources().getString(R.string.backup_success));
                } catch (IOException e) {
                    e.printStackTrace();
                    Util.showSnackbar(BackupChatActivity.this, getResources().getString(R.string.backup_failed));
                }
                progressDialog.dismiss();
                setLastBackupTime();
            }
        });

    }

    private void setLastBackupTime() {
        long lastBackupTime = SharedPreferencesManager.getLastBackup();
        if (lastBackupTime != -1) {
            tvLastBackup.setVisibility(View.VISIBLE);
            String backupTimeStr = TimeHelper.getLastBackupTime(lastBackupTime);
            tvLastBackup.setText(backupTimeStr);

        } else
            tvLastBackup.setVisibility(View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onBackPressed();

        return super.onOptionsItemSelected(item);
    }
}

package com.devlomi.commune.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDex;

import com.devlomi.commune.R;
import com.devlomi.commune.activities.calling.model.AGEventHandler;
import com.devlomi.commune.activities.calling.model.EngineConfig;
import com.devlomi.commune.activities.calling.model.MyEngineEventHandler;
import com.devlomi.commune.job.FireJobCreator;
import com.evernote.android.job.JobManager;
import com.google.android.gms.ads.MobileAds;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.ios.IosEmojiProvider;

import io.agora.rtc.Constants;
import io.agora.rtc.RtcEngine;
import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by Devlomi on 13/08/2017.
 */

public class MyApp extends Application implements Application.ActivityLifecycleCallbacks {
    private static MyApp mApp = null;
    private static String currentChatId = "";
    private static boolean chatActivityVisible;
    private static boolean phoneCallActivityVisible;
    private static boolean baseActivityVisible;
    private static boolean isCallActive = false;

    public static boolean isChatActivityVisible() {
        return chatActivityVisible;
    }

    public static String getCurrentChatId() {
        return currentChatId;
    }

    private boolean hasMovedToForeground = false;

    public boolean isHasMovedToForeground() {
        return hasMovedToForeground;
    }

    private int activityReferences = 0;
    private boolean isActivityChangingConfigurations = false;

    public static void chatActivityResumed(String chatId) {
        chatActivityVisible = true;
        currentChatId = chatId;
    }

    public static void chatActivityPaused() {
        chatActivityVisible = false;
        currentChatId = "";
    }

    public static boolean isPhoneCallActivityVisible() {
        return phoneCallActivityVisible;
    }

    public static void phoneCallActivityResumed() {
        phoneCallActivityVisible = true;
    }

    public static void phoneCallActivityPaused() {
        phoneCallActivityVisible = false;
    }


    public static boolean isBaseActivityVisible() {
        return baseActivityVisible;
    }

    public static void baseActivityResumed() {
        baseActivityVisible = true;
    }

    public static void baseActivityPaused() {
        baseActivityVisible = false;
    }


    public static void setCallActive(boolean mCallActive) {
        isCallActive = mCallActive;
    }

    public static boolean isIsCallActive() {
        return isCallActive;
    }


    private RtcEngine mRtcEngine;
    private EngineConfig mConfig;
    private MyEngineEventHandler mEventHandler;

    public RtcEngine rtcEngine() {
        return mRtcEngine;
    }

    public EngineConfig config() {
        return mConfig;
    }


    public void addEventHandler(AGEventHandler handler) {
        mEventHandler.addEventHandler(handler);
    }

    public void removeEventHandler(AGEventHandler handler) {
        mEventHandler.removeEventHandler(handler);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //add support for vector drawables on older APIs
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        //init realm
        Realm.init(this);
        //init set realm configs
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                .schemaVersion(MyMigration.SCHEMA_VERSION)
                .migration(new MyMigration())
                .build();
        Realm.setDefaultConfiguration(realmConfiguration);
        //init shared prefs manager
        SharedPreferencesManager.init(this);
        //init evernote job
        JobManager.create(this).addJobCreator(new FireJobCreator());
        EmojiManager.install(new IosEmojiProvider());


        //initialize ads for faster loading in first time
        if (false) //if (getResources().getBoolean(R.bool.are_ads_enabled))
            MobileAds.initialize(this);


        registerActivityLifecycleCallbacks(this);

        createRtcEngine();


        mApp = this;

    }

    public static Context context() {
        return mApp.getApplicationContext();
    }

    //to run multi dex
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {

    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

        if (++activityReferences == 1 && !isActivityChangingConfigurations) {
            hasMovedToForeground = true;

        } else {
            hasMovedToForeground = false;
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

        isActivityChangingConfigurations = activity.isChangingConfigurations();
        if (--activityReferences == 0 && !isActivityChangingConfigurations) {
            // App enters background
            SharedPreferencesManager.setLastActive(System.currentTimeMillis());
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }


    private void createRtcEngine() {

        Context context = getApplicationContext();
        String appId = context.getString(R.string.agora_app_id);
        if (TextUtils.isEmpty(appId)) {
            throw new RuntimeException("NEED TO use your App ID, get your own ID at https://dashboard.agora.io/");
        }

        mEventHandler = new MyEngineEventHandler();
        try {
            // Creates an RtcEngine instance
            mRtcEngine = RtcEngine.create(context, appId, mEventHandler);
        } catch (Exception e) {
            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }

        /*
          Sets the channel profile of the Agora RtcEngine.
          The Agora RtcEngine differentiates channel profiles and applies different optimization
          algorithms accordingly. For example, it prioritizes smoothness and low latency for a
          video call, and prioritizes video quality for a video broadcast.
         */
        mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);


//        /*
//          Enables the onAudioVolumeIndication callback at a set time interval to report on which
//          users are speaking and the speakers' volume.
//          Once this method is enabled, the SDK returns the volume indication in the
//          onAudioVolumeIndication callback at the set time interval, regardless of whether any user
//          is speaking in the channel.
//         */
//        mRtcEngine.enableAudioVolumeIndication(200, 3, false);

        mConfig = new EngineConfig();
    }

}

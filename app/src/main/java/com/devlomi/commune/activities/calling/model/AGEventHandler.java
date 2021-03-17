/*
 * Created by Devlomi on 2020
 */

package com.devlomi.commune.activities.calling.model;

public interface AGEventHandler {

    int EVENT_TYPE_ON_DATA_CHANNEL_MSG = 3;

    int EVENT_TYPE_ON_USER_VIDEO_MUTED = 6;

    int EVENT_TYPE_ON_USER_AUDIO_MUTED = 7;

    int EVENT_TYPE_ON_SPEAKER_STATS = 8;

    int EVENT_TYPE_ON_AGORA_MEDIA_ERROR = 9;

    int EVENT_TYPE_ON_USER_VIDEO_STATS = 10;

    int EVENT_TYPE_ON_APP_ERROR = 13;

    int EVENT_TYPE_ON_AUDIO_ROUTE_CHANGED = 18;
    int EVENT_TYPE_CONNECTION_STATE_CHANGED = 19;
}

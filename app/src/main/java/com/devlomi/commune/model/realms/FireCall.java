package com.devlomi.commune.model.realms;

import com.devlomi.commune.activities.calling.model.CallType;
import com.devlomi.commune.utils.TimeHelper;
import com.devlomi.commune.utils.network.FireManager;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class FireCall extends RealmObject {
    @PrimaryKey
    private String callId;
    private User user;
    private int direction;
    private long timestamp;
    private int duration;
    private String phoneNumber;
    private boolean isVideo;
    private int callType;
    private String channel;


    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public boolean isGroupCall(){
        return callType == CallType.CONFERENCE_VIDEO.getValue() || callType == CallType.CONFERENCE_VOICE.getValue();
    }

    public int getCallType() {
        return callType;
    }

    public void setCallType(int callType) {
        this.callType = callType;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        //convert it to milliseconds if needed
        if (!TimeHelper.isTimestampInMillis(timestamp))
            this.timestamp = timestamp * 1000;
        else
            this.timestamp = timestamp;
    }

    public int getDuration() {
        return duration;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isVideo() {
        return isVideo;
    }

    public void setVideo(boolean video) {
        isVideo = video;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public FireCall() {
    }


    public Map toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", ServerValue.TIMESTAMP);
        result.put("callType", callType);
        result.put("callId", callId);
        result.put("callerId", FireManager.getUid());
        result.put("phoneNumber", phoneNumber);
        result.put("toId", user.getUid());
        result.put("channel", channel);
        return result;
    }

    public FireCall(String callId, User user, int direction, long timestamp, String phoneNumber, boolean isVideo, int callType,String channel) {
        this.callId = callId;
        this.user = user;
        this.direction = direction;
        this.phoneNumber = phoneNumber;
        this.isVideo = isVideo;
        this.callType = callType;
        this.channel = channel;
        //convert it to milliseconds if needed
        if (!TimeHelper.isTimestampInMillis(timestamp))
            this.timestamp = timestamp * 1000;
        else
            this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "FireCall{" +
                "callId='" + callId + '\'' +
                ", user=" + user +
                ", type=" + direction +
                ", timestamp=" + timestamp +
                ", duration=" + duration +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", isVideo=" + isVideo +
                '}';
    }


}

package com.devlomi.commune.utils;

import com.devlomi.commune.model.realms.TextStatus;
import com.devlomi.commune.model.constants.StatusType;
import com.devlomi.commune.model.realms.Status;
import com.devlomi.commune.utils.network.FireManager;

import java.util.Date;

public class StatusCreator {
    public static Status createImageStatus(String imagePath) {
        String statusId = FireConstants.getMyStatusRef(StatusType.IMAGE).push().getKey();
        String thumbImg = BitmapUtils.decodeImage(imagePath, false);
        Status status = new Status(statusId, FireManager.getUid(), new Date().getTime(), thumbImg, null, imagePath, StatusType.IMAGE);
        RealmHelper.getInstance().saveObjectToRealm(status);
        return status;
    }

    public static Status createVideoStatus(String videoPath) {
        String statusId = FireConstants.getMyStatusRef(StatusType.VIDEO).push().getKey();
        String thumbImg = BitmapUtils.generateVideoThumbAsBase64(videoPath);
        long mediaLengthInMillis = Util.getMediaLengthInMillis(MyApp.context(), videoPath);
        Status status = new Status(statusId, FireManager.getUid(), new Date().getTime(), thumbImg, null, videoPath, StatusType.VIDEO, mediaLengthInMillis);
        RealmHelper.getInstance().saveObjectToRealm(status);
        return status;
    }

    public static Status createTextStatus(TextStatus textStatus) {
        String statusId = FireConstants.getMyStatusRef(StatusType.TEXT).push().getKey();
        Status status = new Status(statusId, FireManager.getUid(), new Date().getTime(), textStatus,StatusType.TEXT);
        textStatus.setStatusId(statusId);
        RealmHelper.getInstance().saveObjectToRealm(status);
        return status;
    }
}

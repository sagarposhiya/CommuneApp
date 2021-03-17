package com.devlomi.commune.job;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;

import androidx.annotation.RequiresApi;

import com.devlomi.commune.model.realms.GroupEvent;
import com.devlomi.commune.model.realms.JobId;
import com.devlomi.commune.model.realms.Message;
import com.devlomi.commune.utils.DownloadManager;
import com.devlomi.commune.utils.IntentUtils;
import com.devlomi.commune.utils.JobSchedulerSingleton;
import com.devlomi.commune.utils.RealmHelper;
import com.devlomi.commune.utils.network.CallsManager;
import com.devlomi.commune.utils.network.GroupManager;

import java.util.List;

import io.reactivex.disposables.CompositeDisposable;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class NetworkJobService extends BaseJob {
    CompositeDisposable disposables = new CompositeDisposable();
    GroupManager groupManager = new GroupManager();
    CallsManager callsManager = new CallsManager();

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        PersistableBundle extras = jobParameters.getExtras();
        String action = extras.getString(IntentUtils.ACTION_TYPE);
        final boolean isVoiceMessage = isVoiceMessage(jobParameters);

        if (action.equals(IntentUtils.INTENT_ACTION_UPDATE_GROUP)) {

            PersistableBundle groupEventBundle = extras.getPersistableBundle(IntentUtils.EXTRA_GROUP_EVENT);
            String contextStart = groupEventBundle.getString(IntentUtils.EXTRA_CONTEXT_START);
            int eventType = groupEventBundle.getInt(IntentUtils.EXTRA_EVENT_TYPE);
            String contextEnd = groupEventBundle.getString(IntentUtils.EXTRA_CONTEXT_END);
            GroupEvent groupEvent = new GroupEvent(contextStart, eventType, contextEnd);
            final String groupId = extras.getString(IntentUtils.EXTRA_GROUP_ID);

            disposables.add(groupManager.updateGroup(groupId, groupEvent).subscribe(user -> {
                RealmHelper.getInstance().deletePendingGroupCreationJob(groupId);
                onFinishJob(jobParameters, false);

            }, throwable -> {
                onFinishJob(jobParameters, true);

            }));

        } else if (action.equals(IntentUtils.INTENT_ACTION_FETCH_AND_CREATE_GROUP)) {
            String groupId = extras.getString(IntentUtils.EXTRA_GROUP_ID);
            disposables.add(groupManager.fetchAndCreateGroup(groupId).subscribe(user -> {
                if (groupId != null) {
                    RealmHelper.getInstance().deletePendingGroupCreationJob(groupId);
                }
                onFinishJob(jobParameters, groupId == null);

            }, throwable -> {
                onFinishJob(jobParameters, groupId == null);

            }));


        } else if (action.equals(IntentUtils.INTENT_ACTION_FETCH_USER_GROUPS_AND_BROADCASTS)) {
            //we are keeping this since some users may still have this on older versions of the app
            onFinishJob(jobParameters, false);

        } else if (action.equals(IntentUtils.INTENT_ACTION_SET_CALL_ENDED)) {
            String callId = extras.getString(IntentUtils.CALL_ID);
            String otherUid = extras.getString(IntentUtils.OTHER_UID);
            boolean isIncoming = extras.getBoolean(IntentUtils.IS_INCOMING, true);
            disposables.add(
                    callsManager.setCallEnded(callId, otherUid, isIncoming).subscribe(() -> {
                        onFinishJob(jobParameters, false);
                    }, throwable -> {
                        onFinishJob(jobParameters, true);
                    })

            );
        } else if (action.equals(IntentUtils.INTENT_ACTION_SET_CALL_DECLINED_FOR_GROUP)) {
            String callId = extras.getString(IntentUtils.CALL_ID);
            String groupId = extras.getString(IntentUtils.EXTRA_GROUP_ID);
            disposables.add(
                    callsManager.setCallRejectedForGroup(callId, groupId).subscribe(() -> {
                        onFinishJob(jobParameters, false);
                    }, throwable -> {
                        onFinishJob(jobParameters, true);
                    })
            );
        } else {
            final String messageId = extras.getString(IntentUtils.EXTRA_MESSAGE_ID);
            final String chatId = extras.getString(IntentUtils.EXTRA_CHAT_ID);
            if (action.equals(IntentUtils.INTENT_ACTION_UPDATE_MESSAGE_STATE)) {

                final String myUid = extras.getString(IntentUtils.EXTRA_MY_UID);
                final int state = extras.getInt(IntentUtils.EXTRA_STAT, 0);

                disposables.add(fireManager.updateMessagesState(myUid, messageId, state, isVoiceMessage).subscribe(() -> {
                    jobFinished(jobParameters, false);
                }, throwable -> {
                    jobFinished(jobParameters, true);
                }));

            } else if (action.equals(IntentUtils.INTENT_ACTION_UPDATE_VOICE_MESSAGE_STATE)) {
                final String myUid = extras.getString(IntentUtils.EXTRA_MY_UID);
                disposables.add(fireManager.updateVoiceMessageStat(myUid, messageId).subscribe(() -> {
                    jobFinished(jobParameters, false);
                }, throwable -> {
                    jobFinished(jobParameters, true);
                }));

            } else if (action.equals(IntentUtils.INTENT_ACTION_HANDLE_REPLY)) {
                final Message message = RealmHelper.getInstance().getMessage(messageId, chatId);
                if (message != null) {
                    DownloadManager.sendMessage(message, new DownloadManager.OnComplete() {
                        @Override
                        public void onComplete(boolean isSuccessful) {
                            if (isSuccessful) {
                                //set other unread messages as read
                                if (!message.isGroup())
                                    fireManager.setMessagesAsRead(NetworkJobService.this, message.getChatId());

                            }
                            onFinishJob(jobParameters, !isSuccessful);

                        }
                    });
                }
            } else {
                Message message = RealmHelper.getInstance().getMessage(messageId, chatId);
                if (message != null) {
                    DownloadManager.request(message, new DownloadManager.OnComplete() {
                        @Override
                        public void onComplete(boolean isSuccess) {
                            onFinishJob(jobParameters, !isSuccess);
                        }
                    });
                }
            }
        }


        return true;
    }

    private boolean isVoiceMessage(JobParameters jobParameters) {
        return jobParameters.getExtras().getString(IntentUtils.ACTION_TYPE).equals(IntentUtils.INTENT_ACTION_UPDATE_VOICE_MESSAGE_STATE);
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        int jobId = jobParameters.getJobId();
        boolean isVoiceMessage = isVoiceMessage(jobParameters);
        String id = RealmHelper.getInstance().getJobId(jobId, isVoiceMessage);

        disposables.dispose();

        return true;
    }

    public static void schedule(Context context, String id, PersistableBundle bundle) {
        ComponentName component = new ComponentName(context, NetworkJobService.class);


        String action = bundle.getString(IntentUtils.ACTION_TYPE);

        //if it's a voice message then we want to generate a new id
        // because in case if  the action was 'update message state' both will have the same id

        JobId jobId = new JobId(id, action.equals(IntentUtils.INTENT_ACTION_UPDATE_VOICE_MESSAGE_STATE));
        RealmHelper.getInstance().saveJobId(jobId);
        int mJobId = jobId.getJobId();

        JobInfo.Builder builder = new JobInfo.Builder(mJobId, component)
                .setMinimumLatency(1)
                .setOverrideDeadline(1)
                .setPersisted(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setExtras(bundle);
        List<JobInfo> allPendingJobs = JobSchedulerSingleton.getInstance().getAllPendingJobs();

        //only 100 job is allowed
        if (allPendingJobs.size() < 95)
            JobSchedulerSingleton.getInstance().schedule(builder.build());


    }


    private void onFinishJob(JobParameters jobParameters, boolean needsReschedule) {
        if (!needsReschedule) {
            String id = jobParameters.getExtras().getString(IntentUtils.ID);
            RealmHelper.getInstance().deleteJobId(id, isVoiceMessage(jobParameters));
        }
        jobFinished(jobParameters, needsReschedule);
    }

    public static void cancel(String messageId) {
        int jobId = RealmHelper.getInstance().getJobId(messageId, false);

        if (jobId != -1) {
            JobSchedulerSingleton.getInstance().cancel(jobId);

        }
    }
}

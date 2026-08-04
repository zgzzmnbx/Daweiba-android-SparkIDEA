package com.dabawei.flashnote;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

public final class BackgroundSyncScheduler {
    public static final int JOB_ID = 24081;

    private BackgroundSyncScheduler() {
    }

    public static void ensureScheduled(Context context) {
        Context appContext = context.getApplicationContext();
        if (!ReminderSettings.isBackgroundSyncEnabled(appContext)) {
            cancel(appContext);
            return;
        }
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            JobScheduler scheduler = (JobScheduler) appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (scheduler == null) {
                return;
            }
            JobInfo job = new JobInfo.Builder(
                    JOB_ID,
                    new ComponentName(appContext, TodoSyncJobService.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(ReminderSettings.getBackgroundSyncIntervalMillis())
                    .setPersisted(true)
                    .build();
            scheduler.schedule(job);
            return;
        }
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            PendingIntent intent = ReminderScheduler.buildBackgroundSyncPendingIntent(appContext);
            alarmManager.cancel(intent);
            alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + ReminderSettings.getBackgroundSyncIntervalMillis(),
                    ReminderSettings.getBackgroundSyncIntervalMillis(),
                    intent);
        }
    }

    public static void cancel(Context context) {
        Context appContext = context.getApplicationContext();
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            JobScheduler scheduler = (JobScheduler) appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (scheduler != null) {
                scheduler.cancel(JOB_ID);
            }
        }
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(ReminderScheduler.buildBackgroundSyncPendingIntent(appContext));
        }
    }
}

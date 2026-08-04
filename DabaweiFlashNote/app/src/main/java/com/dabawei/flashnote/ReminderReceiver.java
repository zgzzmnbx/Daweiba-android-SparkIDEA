package com.dabawei.flashnote;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class ReminderReceiver extends BroadcastReceiver {
    public static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    public static final String ACTION_TIME_SET = "android.intent.action.TIME_SET";
    public static final String ACTION_TIMEZONE_CHANGED = "android.intent.action.TIMEZONE_CHANGED";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? "" : intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || ACTION_TIME_SET.equals(action)
                || ACTION_TIMEZONE_CHANGED.equals(action)) {
            new ReminderScheduler(context).rescheduleAll();
            return;
        }
        if (ReminderScheduler.ACTION_SNOOZE.equals(action)) {
            handleSnooze(context, intent);
            return;
        }
        if (ReminderScheduler.ACTION_FIRE.equals(action)) {
            handleFire(context, intent);
        }
    }

    private void handleFire(Context context, Intent intent) {
        String taskId = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_ID);
        FlashNoteDatabase database = new FlashNoteDatabase(context);
        ReminderRecord record = database.getReminderByTaskId(taskId);
        if (record == null || ReminderRecord.STATUS_CANCELLED.equals(record.getStatus())) {
            database.close();
            return;
        }
        long now = System.currentTimeMillis();
        long targetAt = record.getSnoozeUntil() > 0L ? record.getSnoozeUntil() : record.getRemindAt();
        if (targetAt > now + 1000L) {
            new ReminderScheduler(context, database).schedule(record);
            database.close();
            return;
        }

        ReminderRecord fired = record.withStatus(ReminderRecord.STATUS_FIRED, 0L).withLastSyncedAt(now);
        database.upsertReminder(fired);
        showNotification(context, fired, targetAt);
        database.close();
    }

    private void handleSnooze(Context context, Intent intent) {
        String taskId = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_ID);
        int minutes = intent.getIntExtra(ReminderScheduler.EXTRA_SNOOZE_MINUTES, 10);
        if (minutes != 10 && minutes != 60) {
            minutes = 10;
        }
        FlashNoteDatabase database = new FlashNoteDatabase(context);
        ReminderRecord record = database.getReminderByTaskId(taskId);
        if (record == null || ReminderRecord.STATUS_CANCELLED.equals(record.getStatus())) {
            database.close();
            return;
        }
        long snoozeUntil = System.currentTimeMillis() + minutes * 60L * 1000L;
        ReminderRecord snoozed = record
                .withStatus(ReminderRecord.STATUS_SNOOZED, snoozeUntil)
                .withLastSyncedAt(System.currentTimeMillis());
        database.upsertReminder(snoozed);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(record.getNotificationId());
        }
        new ReminderScheduler(context, database).schedule(snoozed);
        database.close();
    }

    private void showNotification(Context context, ReminderRecord record, long triggeredAt) {
        ReminderScheduler.ensureNotificationChannel(context);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }

        Intent viewIntent = new Intent(context, MainActivity.class)
                .putExtra(ReminderScheduler.EXTRA_OPEN_TODO, true)
                .putExtra(ReminderScheduler.EXTRA_TASK_ID, record.getTaskId())
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int viewFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            viewFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent viewPendingIntent = PendingIntent.getActivity(
                context,
                record.getNotificationId(),
                viewIntent,
                viewFlags);

        PendingIntent snoozeTen = ReminderScheduler.buildSnoozePendingIntent(context, record, 10);
        PendingIntent snoozeHour = ReminderScheduler.buildSnoozePendingIntent(context, record, 60);
        String source = displaySource(record.getSourcePath());
        String when = TodoDateTime.format(triggeredAt > 0L ? triggeredAt : record.getRemindAt());
        String detail = when + " · 来自《" + source + "》";
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, ReminderScheduler.REMINDER_CHANNEL_ID)
                : new Notification.Builder(context);
        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(record.getTaskText())
                .setContentText(detail)
                .setStyle(new Notification.BigTextStyle().bigText(record.getTaskText() + "\n" + detail))
                .setContentIntent(viewPendingIntent)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setPriority(Notification.PRIORITY_HIGH)
                .addAction(new Notification.Action.Builder(null, "稍后10分钟", snoozeTen).build())
                .addAction(new Notification.Action.Builder(null, "稍后1小时", snoozeHour).build())
                .addAction(new Notification.Action.Builder(null, "查看待办", viewPendingIntent).build());
        try {
            manager.notify(record.getNotificationId(), builder.build());
        } catch (SecurityException ignored) {
            // Android 13+ notification permission can be denied after the alarm was scheduled.
        }
    }

    private String displaySource(String sourcePath) {
        String source = sourcePath == null ? "Obsidian" : sourcePath.replace('\\', '/');
        int slash = source.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < source.length()) {
            source = source.substring(slash + 1);
        }
        if (source.endsWith(".md")) {
            source = source.substring(0, source.length() - 3);
        }
        return source.length() == 0 ? "Obsidian" : source;
    }
}

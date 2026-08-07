package com.dabawei.flashnote;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.List;

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
            rescheduleStoredAlarms(context);
            BackgroundSyncScheduler.ensureScheduled(context);
            return;
        }
        if (ReminderScheduler.ACTION_DAILY_OVERVIEW.equals(action)) {
            handleDailyOverview(context);
            return;
        }
        if (ReminderScheduler.ACTION_BACKGROUND_SYNC.equals(action)) {
            handleBackgroundSync(context);
            return;
        }
        if (ReminderScheduler.ACTION_SNOOZE.equals(action)) {
            if (getOccurrenceId(intent) > 0L) {
                handleOccurrenceSnooze(context, intent);
            } else {
                handleSnooze(context, intent);
            }
            return;
        }
        if (ReminderScheduler.ACTION_FIRE.equals(action)) {
            if (getOccurrenceId(intent) > 0L) {
                handleOccurrenceFire(context, intent);
            } else {
                handleFire(context, intent);
            }
        }
    }

    private void rescheduleStoredAlarms(Context context) {
        FlashNoteDatabase database = new FlashNoteDatabase(context);
        try {
            new ReminderScheduler(context, database).rescheduleAll();
        } finally {
            database.close();
        }
    }

    private void handleFire(Context context, Intent intent) {
        String taskId = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_ID);
        FlashNoteDatabase database = new FlashNoteDatabase(context);
        try {
            ReminderRecord record = database.getReminderByTaskId(taskId);
            if (record == null || ReminderRecord.STATUS_CANCELLED.equals(record.getStatus())) {
                return;
            }
            long now = System.currentTimeMillis();
            long targetAt = record.getSnoozeUntil() > 0L ? record.getSnoozeUntil() : record.getRemindAt();
            if (targetAt > now + 1000L) {
                new ReminderScheduler(context, database).schedule(record);
                return;
            }

            ReminderRecord fired = record.withStatus(ReminderRecord.STATUS_FIRED, 0L).withLastSyncedAt(now);
            database.upsertReminder(fired);
            showReminderNotification(
                    context,
                    fired.getTaskId(),
                    fired.getTaskText(),
                    fired.getSourcePath(),
                    fired.getNotificationId(),
                    targetAt,
                    ReminderScheduler.buildSnoozePendingIntent(context, fired, 10),
                    ReminderScheduler.buildSnoozePendingIntent(context, fired, 60));
            sendFeishuReminder(context, fired.getTaskText(), fired.getSourcePath(), targetAt);
        } finally {
            database.close();
        }
    }

    private void handleOccurrenceFire(Context context, Intent intent) {
        long occurrenceId = getOccurrenceId(intent);
        FlashNoteDatabase database = new FlashNoteDatabase(context);
        try {
            ReminderOccurrence occurrence = database.getReminderOccurrenceById(occurrenceId);
            ReminderRecord parent = occurrence == null
                    ? null
                    : database.getReminderByTaskId(occurrence.getTaskId());
            if (occurrence == null
                    || parent == null
                    || ReminderRecord.STATUS_CANCELLED.equals(occurrence.getStatus())
                    || ReminderRecord.STATUS_CANCELLED.equals(parent.getStatus())) {
                return;
            }
            long now = System.currentTimeMillis();
            long targetAt = occurrence.getSnoozeUntil() > 0L
                    ? occurrence.getSnoozeUntil()
                    : occurrence.getTriggerAt();
            if (targetAt > now + 1000L) {
                new ReminderScheduler(context, database).schedule(occurrence);
                return;
            }
            ReminderOccurrence fired = occurrence.withStatus(
                    ReminderRecord.STATUS_FIRED,
                    0L).withLastSyncedAt(now);
            database.upsertReminderOccurrence(fired);
            showReminderNotification(
                    context,
                    parent.getTaskId(),
                    parent.getTaskText(),
                    parent.getSourcePath(),
                    fired.getNotificationId(),
                    targetAt,
                    ReminderScheduler.buildSnoozePendingIntent(context, fired, 10),
                    ReminderScheduler.buildSnoozePendingIntent(context, fired, 60));
            sendFeishuReminder(context, parent.getTaskText(), parent.getSourcePath(), targetAt);
        } finally {
            database.close();
        }
    }

    private void handleSnooze(Context context, Intent intent) {
        String taskId = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_ID);
        int minutes = normalizeSnoozeMinutes(intent);
        FlashNoteDatabase database = new FlashNoteDatabase(context);
        try {
            ReminderRecord record = database.getReminderByTaskId(taskId);
            if (record == null || ReminderRecord.STATUS_CANCELLED.equals(record.getStatus())) {
                return;
            }
            long now = System.currentTimeMillis();
            ReminderRecord snoozed = record
                    .withStatus(ReminderRecord.STATUS_SNOOZED, now + minutes * 60L * 1000L)
                    .withLastSyncedAt(now);
            database.upsertReminder(snoozed);
            new ReminderScheduler(context, database).cancel(record);
            new ReminderScheduler(context, database).schedule(snoozed);
        } finally {
            database.close();
        }
    }

    private void handleOccurrenceSnooze(Context context, Intent intent) {
        int minutes = normalizeSnoozeMinutes(intent);
        FlashNoteDatabase database = new FlashNoteDatabase(context);
        try {
            ReminderOccurrence occurrence = database.getReminderOccurrenceById(getOccurrenceId(intent));
            if (occurrence == null || ReminderRecord.STATUS_CANCELLED.equals(occurrence.getStatus())) {
                return;
            }
            long now = System.currentTimeMillis();
            ReminderOccurrence snoozed = occurrence
                    .withStatus(ReminderRecord.STATUS_SNOOZED, now + minutes * 60L * 1000L)
                    .withLastSyncedAt(now);
            database.upsertReminderOccurrence(snoozed);
            ReminderScheduler scheduler = new ReminderScheduler(context, database);
            scheduler.cancel(occurrence);
            scheduler.schedule(snoozed);
        } finally {
            database.close();
        }
    }

    private void handleDailyOverview(Context context) {
        FlashNoteDatabase database = new FlashNoteDatabase(context);
        try {
            List<TodoSyncItem> items = database.getOverviewTodos(
                    ReminderTimeCalculator.todayEnd(System.currentTimeMillis()));
            if (items.isEmpty()) {
                return;
            }
            ReminderScheduler.ensureNotificationChannel(context);
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) {
                return;
            }
            Intent viewIntent = new Intent(context, MainActivity.class)
                    .putExtra(ReminderScheduler.EXTRA_OPEN_TODO, true)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent viewPendingIntent = PendingIntent.getActivity(
                    context,
                    ReminderIds.DAILY_OVERVIEW_NOTIFICATION_ID,
                    viewIntent,
                    flags);
            boolean privateMode = ReminderSettings.isLockScreenPrivate(context);
            String title = privateMode ? "今日有待办" : "今日待办概览";
            String text = privateMode
                    ? "打开大尾巴闪念查看今日待办"
                    : buildOverviewText(items);
            Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(context, ReminderScheduler.DAILY_OVERVIEW_CHANNEL_ID)
                    : new Notification.Builder(context);
            builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setStyle(new Notification.BigTextStyle().bigText(text))
                    .setContentIntent(viewPendingIntent)
                    .setAutoCancel(true)
                    .setVisibility(privateMode
                            ? Notification.VISIBILITY_PRIVATE
                            : Notification.VISIBILITY_PUBLIC)
                    .setCategory(Notification.CATEGORY_EVENT)
                    .setPriority(Notification.PRIORITY_DEFAULT);
            try {
                manager.notify(ReminderIds.DAILY_OVERVIEW_NOTIFICATION_ID, builder.build());
            } catch (SecurityException ignored) {
                // Android 13+ notification permission can be denied after scheduling.
            }
        } finally {
            database.close();
        }
    }

    private String buildOverviewText(List<TodoSyncItem> items) {
        StringBuilder builder = new StringBuilder();
        builder.append("共 ").append(items.size()).append(" 项");
        int limit = Math.min(5, items.size());
        for (int index = 0; index < limit; index++) {
            builder.append("\n· ").append(items.get(index).getText());
        }
        if (items.size() > limit) {
            builder.append("\n还有 ").append(items.size() - limit).append(" 项");
        }
        return builder.toString();
    }

    private void handleBackgroundSync(final Context context) {
        final PendingResult pendingResult = goAsync();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    TodoSyncCoordinator.syncInNewDatabase(context);
                } finally {
                    pendingResult.finish();
                }
            }
        }).start();
    }

    private int normalizeSnoozeMinutes(Intent intent) {
        int minutes = intent == null
                ? 10
                : intent.getIntExtra(ReminderScheduler.EXTRA_SNOOZE_MINUTES, 10);
        return minutes == 60 ? 60 : 10;
    }

    private void sendFeishuReminder(
            Context context,
            final String taskText,
            final String sourcePath,
            final long triggeredAt) {
        final FeishuSettings settings = FeishuSettings.load(context);
        if (!settings.isReady()) {
            return;
        }
        final PendingResult pendingResult = goAsync();
        final String message = buildFeishuMessage(taskText, sourcePath, triggeredAt);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    FeishuWebhookClient.send(settings.getWebhookUrl(), message);
                } finally {
                    pendingResult.finish();
                }
            }
        }, "dabawei-feishu-reminder").start();
    }

    private String buildFeishuMessage(String taskText, String sourcePath, long triggeredAt) {
        return "大尾巴闪念 · 待办提醒\n"
                + "内容：" + (taskText == null ? "" : taskText) + "\n"
                + "提醒时间：" + TodoDateTime.format(triggeredAt) + "\n"
                + "来源：《" + displaySource(sourcePath) + "》";
    }

    private long getOccurrenceId(Intent intent) {
        return intent == null
                ? 0L
                : intent.getLongExtra(ReminderScheduler.EXTRA_OCCURRENCE_ID, 0L);
    }

    private void showReminderNotification(
            Context context,
            String taskId,
            String taskText,
            String sourcePath,
            int notificationId,
            long triggeredAt,
            PendingIntent snoozeTen,
            PendingIntent snoozeHour) {
        ReminderScheduler.ensureNotificationChannel(context);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        Intent viewIntent = new Intent(context, MainActivity.class)
                .putExtra(ReminderScheduler.EXTRA_OPEN_TODO, true)
                .putExtra(ReminderScheduler.EXTRA_TASK_ID, taskId)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int viewFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            viewFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent viewPendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                viewIntent,
                viewFlags);
        boolean privateMode = ReminderSettings.isLockScreenPrivate(context);
        String title = privateMode ? "你有一条待办提醒" : taskText;
        String detail = privateMode
                ? "打开大尾巴闪念查看详情"
                : TodoDateTime.format(triggeredAt) + " · 来自《" + displaySource(sourcePath) + "》";
        String bigText = privateMode ? detail : taskText + "\n" + detail;
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, ReminderScheduler.REMINDER_CHANNEL_ID)
                : new Notification.Builder(context);
        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(detail)
                .setStyle(new Notification.BigTextStyle().bigText(bigText))
                .setContentIntent(viewPendingIntent)
                .setAutoCancel(true)
                .setVisibility(privateMode
                        ? Notification.VISIBILITY_PRIVATE
                        : Notification.VISIBILITY_PUBLIC)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setPriority(Notification.PRIORITY_HIGH)
                .addAction(new Notification.Action.Builder(null, "稍后10分钟", snoozeTen).build())
                .addAction(new Notification.Action.Builder(null, "稍后1小时", snoozeHour).build())
                .addAction(new Notification.Action.Builder(null, "查看待办", viewPendingIntent).build());
        try {
            manager.notify(notificationId, builder.build());
        } catch (SecurityException ignored) {
            // Android 13+ notification permission can be denied after the alarm was scheduled.
        }
    }

    private static String displaySource(String sourcePath) {
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

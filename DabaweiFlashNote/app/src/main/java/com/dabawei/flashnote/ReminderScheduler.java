package com.dabawei.flashnote;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import java.util.List;
import java.util.TimeZone;

public final class ReminderScheduler {
    public static final String ACTION_FIRE = "com.dabawei.flashnote.action.REMINDER_FIRE";
    public static final String ACTION_SNOOZE = "com.dabawei.flashnote.action.REMINDER_SNOOZE";
    public static final String EXTRA_TASK_ID = "extra_task_id";
    public static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";
    public static final String EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes";
    public static final String EXTRA_OPEN_TODO = "extra_open_todo";
    public static final String REMINDER_CHANNEL_ID = "todo_reminders";

    private final Context context;
    private final FlashNoteDatabase database;
    private final AlarmManager alarmManager;

    public ReminderScheduler(Context context) {
        this.context = context.getApplicationContext();
        this.database = new FlashNoteDatabase(this.context);
        this.alarmManager = (AlarmManager) this.context.getSystemService(Context.ALARM_SERVICE);
        ensureNotificationChannel(this.context);
    }

    public ReminderScheduler(Context context, FlashNoteDatabase database) {
        this.context = context.getApplicationContext();
        this.database = database;
        this.alarmManager = (AlarmManager) this.context.getSystemService(Context.ALARM_SERVICE);
        ensureNotificationChannel(this.context);
    }

    public ScheduleResult schedule(ReminderRecord record) {
        if (record == null || alarmManager == null || record.getTaskId().length() == 0) {
            return ScheduleResult.notScheduled(false);
        }
        long targetAt = record.getSnoozeUntil() > 0L ? record.getSnoozeUntil() : record.getRemindAt();
        if (!ReminderRecord.STATUS_SCHEDULED.equals(record.getStatus())
                && !ReminderRecord.STATUS_SNOOZED.equals(record.getStatus())) {
            cancel(record);
            return ScheduleResult.notScheduled(isExactAlarmAllowed(context));
        }
        if (targetAt <= System.currentTimeMillis()) {
            cancel(record);
            return ScheduleResult.notScheduled(isExactAlarmAllowed(context));
        }

        PendingIntent pendingIntent = buildFirePendingIntent(context, record);
        boolean exact = isExactAlarmAllowed(context);
        try {
            if (exact) {
                if (Build.VERSION.SDK_INT >= 23) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetAt, pendingIntent);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, targetAt, pendingIntent);
                }
            } else if (Build.VERSION.SDK_INT >= 23) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetAt, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, targetAt, pendingIntent);
            }
            return new ScheduleResult(true, exact);
        } catch (SecurityException securityException) {
            try {
                if (Build.VERSION.SDK_INT >= 23) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetAt, pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, targetAt, pendingIntent);
                }
                return new ScheduleResult(true, false);
            } catch (RuntimeException ignored) {
                return ScheduleResult.notScheduled(false);
            }
        }
    }

    public void cancel(ReminderRecord record) {
        if (record == null || alarmManager == null) {
            return;
        }
        alarmManager.cancel(buildFirePendingIntent(context, record));
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(record.getNotificationId());
        }
    }

    public void rescheduleAll() {
        List<ReminderRecord> records = database.getSchedulableReminders();
        long now = System.currentTimeMillis();
        String currentTimeZone = TimeZone.getDefault().getID();
        for (ReminderRecord record : records) {
            ReminderRecord adjusted = record;
            if (record.getRemindAtText().length() > 0) {
                long recalculated = TodoDateTime.parseDateTime(record.getRemindAtText());
                if (recalculated > 0L
                        && (recalculated != record.getRemindAt()
                        || !currentTimeZone.equals(record.getTimeZoneId()))) {
                    adjusted = record.withRemindAt(recalculated, currentTimeZone);
                    database.upsertReminder(adjusted);
                }
            }
            long targetAt = adjusted.getSnoozeUntil() > 0L
                    ? adjusted.getSnoozeUntil()
                    : adjusted.getRemindAt();
            if (targetAt <= now) {
                cancel(adjusted);
                database.upsertReminder(adjusted.withStatus(ReminderRecord.STATUS_OVERDUE, 0L));
            } else {
                schedule(adjusted);
            }
        }
    }

    public static boolean isExactAlarmAllowed(Context context) {
        if (Build.VERSION.SDK_INT < 31) {
            return true;
        }
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return manager != null && manager.canScheduleExactAlarms();
    }

    public static void ensureNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                REMINDER_CHANNEL_ID,
                "待办提醒",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("大尾巴闪念待办的单条提醒");
        manager.createNotificationChannel(channel);
    }

    static PendingIntent buildFirePendingIntent(Context context, ReminderRecord record) {
        Intent intent = new Intent(context, ReminderReceiver.class)
                .setAction(ACTION_FIRE)
                .setData(Uri.parse("dabawei://reminder/" + record.getNotificationId()))
                .putExtra(EXTRA_TASK_ID, record.getTaskId())
                .putExtra(EXTRA_NOTIFICATION_ID, record.getNotificationId());
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, record.getNotificationId(), intent, flags);
    }

    static PendingIntent buildSnoozePendingIntent(Context context, ReminderRecord record, int minutes) {
        Intent intent = new Intent(context, ReminderReceiver.class)
                .setAction(ACTION_SNOOZE)
                .setData(Uri.parse("dabawei://reminder/snooze/" + record.getNotificationId() + "/" + minutes))
                .putExtra(EXTRA_TASK_ID, record.getTaskId())
                .putExtra(EXTRA_NOTIFICATION_ID, record.getNotificationId())
                .putExtra(EXTRA_SNOOZE_MINUTES, minutes);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, record.getNotificationId() + minutes, intent, flags);
    }

    public static final class ScheduleResult {
        private final boolean scheduled;
        private final boolean exact;

        private ScheduleResult(boolean scheduled, boolean exact) {
            this.scheduled = scheduled;
            this.exact = exact;
        }

        static ScheduleResult notScheduled(boolean exact) {
            return new ScheduleResult(false, exact);
        }

        public boolean isScheduled() {
            return scheduled;
        }

        public boolean isExact() {
            return exact;
        }
    }
}

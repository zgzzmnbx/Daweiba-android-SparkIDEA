package com.dabawei.flashnote;

public final class ReminderOccurrence {
    public static final String KIND_DAY_BEFORE = "day_before";
    public static final String KIND_HOUR_BEFORE = "hour_before";

    private final long occurrenceId;
    private final String taskId;
    private final String kind;
    private final long triggerAt;
    private final long snoozeUntil;
    private final String status;
    private final int notificationId;
    private final long lastSyncedAt;
    private final String timeZoneId;

    public ReminderOccurrence(
            long occurrenceId,
            String taskId,
            String kind,
            long triggerAt,
            long snoozeUntil,
            String status,
            int notificationId,
            long lastSyncedAt,
            String timeZoneId) {
        this.occurrenceId = occurrenceId;
        this.taskId = safe(taskId);
        this.kind = safe(kind);
        this.triggerAt = triggerAt;
        this.snoozeUntil = snoozeUntil;
        this.status = safe(status).length() == 0 ? ReminderRecord.STATUS_CANCELLED : status;
        this.notificationId = notificationId;
        this.lastSyncedAt = lastSyncedAt;
        this.timeZoneId = safe(timeZoneId);
    }

    public long getOccurrenceId() {
        return occurrenceId;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getKind() {
        return kind;
    }

    public long getTriggerAt() {
        return triggerAt;
    }

    public long getSnoozeUntil() {
        return snoozeUntil;
    }

    public String getStatus() {
        return status;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public long getLastSyncedAt() {
        return lastSyncedAt;
    }

    public String getTimeZoneId() {
        return timeZoneId;
    }

    public ReminderOccurrence withStatus(String nextStatus, long nextSnoozeUntil) {
        return copy(occurrenceId, triggerAt, nextSnoozeUntil, nextStatus, timeZoneId, lastSyncedAt);
    }

    public ReminderOccurrence withTriggerAt(long nextTriggerAt, String nextTimeZoneId) {
        return copy(occurrenceId, nextTriggerAt, snoozeUntil, status, nextTimeZoneId, lastSyncedAt);
    }

    public ReminderOccurrence withLastSyncedAt(long nextLastSyncedAt) {
        return copy(occurrenceId, triggerAt, snoozeUntil, status, timeZoneId, nextLastSyncedAt);
    }

    private ReminderOccurrence copy(
            long nextOccurrenceId,
            long nextTriggerAt,
            long nextSnoozeUntil,
            String nextStatus,
            String nextTimeZoneId,
            long nextLastSyncedAt) {
        return new ReminderOccurrence(
                nextOccurrenceId,
                taskId,
                kind,
                nextTriggerAt,
                nextSnoozeUntil,
                nextStatus,
                notificationId,
                nextLastSyncedAt,
                nextTimeZoneId);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

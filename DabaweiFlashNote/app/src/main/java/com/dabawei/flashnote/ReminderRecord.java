package com.dabawei.flashnote;

public final class ReminderRecord {
    public static final String STATUS_SCHEDULED = "scheduled";
    public static final String STATUS_FIRED = "fired";
    public static final String STATUS_SNOOZED = "snoozed";
    public static final String STATUS_CANCELLED = "cancelled";
    public static final String STATUS_OVERDUE = "overdue";

    private final long reminderId;
    private final String taskId;
    private final long localNoteId;
    private final String taskText;
    private final String sourcePath;
    private final String sourceBlockId;
    private final long dueAt;
    private final long remindAt;
    private final long snoozeUntil;
    private final String status;
    private final int notificationId;
    private final long lastSyncedAt;
    private final String dueAtText;
    private final String remindAtText;
    private final String remoteRemindAtText;
    private final String timeZoneId;

    public ReminderRecord(
            long reminderId,
            String taskId,
            long localNoteId,
            String taskText,
            String sourcePath,
            String sourceBlockId,
            long dueAt,
            long remindAt,
            long snoozeUntil,
            String status,
            int notificationId,
            long lastSyncedAt,
            String dueAtText,
            String remindAtText,
            String timeZoneId) {
        this(
                reminderId,
                taskId,
                localNoteId,
                taskText,
                sourcePath,
                sourceBlockId,
                dueAt,
                remindAt,
                snoozeUntil,
                status,
                notificationId,
                lastSyncedAt,
                dueAtText,
                remindAtText,
                remindAtText,
                timeZoneId);
    }

    public ReminderRecord(
            long reminderId,
            String taskId,
            long localNoteId,
            String taskText,
            String sourcePath,
            String sourceBlockId,
            long dueAt,
            long remindAt,
            long snoozeUntil,
            String status,
            int notificationId,
            long lastSyncedAt,
            String dueAtText,
            String remindAtText,
            String remoteRemindAtText,
            String timeZoneId) {
        this.reminderId = reminderId;
        this.taskId = safe(taskId);
        this.localNoteId = localNoteId;
        this.taskText = safe(taskText);
        this.sourcePath = safe(sourcePath);
        this.sourceBlockId = safe(sourceBlockId);
        this.dueAt = dueAt;
        this.remindAt = remindAt;
        this.snoozeUntil = snoozeUntil;
        this.status = safe(status).length() == 0 ? STATUS_CANCELLED : status;
        this.notificationId = notificationId;
        this.lastSyncedAt = lastSyncedAt;
        this.dueAtText = safe(dueAtText);
        this.remindAtText = safe(remindAtText);
        this.remoteRemindAtText = safe(remoteRemindAtText);
        this.timeZoneId = safe(timeZoneId);
    }

    public long getReminderId() {
        return reminderId;
    }

    public String getTaskId() {
        return taskId;
    }

    public long getLocalNoteId() {
        return localNoteId;
    }

    public String getTaskText() {
        return taskText;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getSourceBlockId() {
        return sourceBlockId;
    }

    public long getDueAt() {
        return dueAt;
    }

    public long getRemindAt() {
        return remindAt;
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

    public String getDueAtText() {
        return dueAtText;
    }

    public String getRemindAtText() {
        return remindAtText;
    }

    public String getRemoteRemindAtText() {
        return remoteRemindAtText;
    }

    public String getTimeZoneId() {
        return timeZoneId;
    }

    public ReminderRecord withStatus(String nextStatus, long nextSnoozeUntil) {
        return copy(reminderId, nextSnoozeUntil, nextStatus, remindAt, timeZoneId, lastSyncedAt);
    }

    public ReminderRecord withLastSyncedAt(long nextLastSyncedAt) {
        return copy(reminderId, snoozeUntil, status, remindAt, timeZoneId, nextLastSyncedAt);
    }

    public ReminderRecord withRemindAt(long nextRemindAt, String nextTimeZoneId) {
        return copy(reminderId, snoozeUntil, status, nextRemindAt, nextTimeZoneId, lastSyncedAt);
    }

    public ReminderRecord withReminderId(long nextReminderId) {
        return copy(nextReminderId, snoozeUntil, status, remindAt, timeZoneId, lastSyncedAt);
    }

    private ReminderRecord copy(
            long nextReminderId,
            long nextSnoozeUntil,
            String nextStatus,
            long nextRemindAt,
            String nextTimeZoneId,
            long nextLastSyncedAt) {
        return new ReminderRecord(
                nextReminderId,
                taskId,
                localNoteId,
                taskText,
                sourcePath,
                sourceBlockId,
                dueAt,
                nextRemindAt,
                nextSnoozeUntil,
                nextStatus,
                notificationId,
                nextLastSyncedAt,
                dueAtText,
                remindAtText,
                remoteRemindAtText,
                nextTimeZoneId);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

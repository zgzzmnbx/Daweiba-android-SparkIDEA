package com.dabawei.flashnote;

public final class ReminderRecord {
    public static final String STATUS_SCHEDULED = "scheduled";
    public static final String STATUS_FIRED = "fired";
    public static final String STATUS_SNOOZED = "snoozed";
    public static final String STATUS_CANCELLED = "cancelled";
    public static final String STATUS_OVERDUE = "overdue";

    public static final String SOURCE_MANUAL = "manual";
    public static final String SOURCE_EXPLICIT = "explicit";
    public static final String SOURCE_NATURAL = "natural_language";
    public static final String SOURCE_DUE_DEFAULT = "due_default";

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
    private final String reminderSource;
    private final String sourceExpression;
    private final String sourceSignature;
    private final long naturalReferenceAt;
    private final boolean autoSuppressed;

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
                timeZoneId,
                defaultSource(remindAtText, remindAtText),
                defaultExpression(remindAtText, remindAtText),
                defaultSignature(taskId, remindAtText, remindAtText),
                0L,
                false);
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
                remoteRemindAtText,
                timeZoneId,
                defaultSource(remindAtText, remoteRemindAtText),
                defaultExpression(remindAtText, remoteRemindAtText),
                defaultSignature(taskId, remindAtText, remoteRemindAtText),
                0L,
                false);
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
            String timeZoneId,
            String reminderSource,
            String sourceExpression,
            String sourceSignature,
            long naturalReferenceAt,
            boolean autoSuppressed) {
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
        String normalizedSource = safe(reminderSource);
        this.reminderSource = normalizedSource.length() == 0
                ? defaultSource(remindAtText, remoteRemindAtText)
                : normalizedSource;
        String normalizedExpression = safe(sourceExpression);
        this.sourceExpression = normalizedExpression.length() == 0
                ? defaultExpression(remindAtText, remoteRemindAtText)
                : normalizedExpression;
        String normalizedSignature = safe(sourceSignature);
        this.sourceSignature = normalizedSignature.length() == 0
                ? defaultSignature(taskId, remindAtText, remoteRemindAtText)
                : normalizedSignature;
        this.naturalReferenceAt = naturalReferenceAt;
        this.autoSuppressed = autoSuppressed;
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

    public String getReminderSource() {
        return reminderSource;
    }

    public String getSourceExpression() {
        return sourceExpression;
    }

    public String getSourceSignature() {
        return sourceSignature;
    }

    public long getNaturalReferenceAt() {
        return naturalReferenceAt;
    }

    public boolean isAutoSuppressed() {
        return autoSuppressed;
    }

    public ReminderRecord withStatus(String nextStatus, long nextSnoozeUntil) {
        return copy(reminderId, nextSnoozeUntil, nextStatus, remindAt, timeZoneId, lastSyncedAt,
                reminderSource, sourceExpression, sourceSignature, naturalReferenceAt, autoSuppressed);
    }

    public ReminderRecord withLastSyncedAt(long nextLastSyncedAt) {
        return copy(reminderId, snoozeUntil, status, remindAt, timeZoneId, nextLastSyncedAt,
                reminderSource, sourceExpression, sourceSignature, naturalReferenceAt, autoSuppressed);
    }

    public ReminderRecord withRemindAt(long nextRemindAt, String nextTimeZoneId) {
        return copy(reminderId, snoozeUntil, status, nextRemindAt, nextTimeZoneId, lastSyncedAt,
                reminderSource, sourceExpression, sourceSignature, naturalReferenceAt, autoSuppressed);
    }

    public ReminderRecord withReminderId(long nextReminderId) {
        return copy(nextReminderId, snoozeUntil, status, remindAt, timeZoneId, lastSyncedAt,
                reminderSource, sourceExpression, sourceSignature, naturalReferenceAt, autoSuppressed);
    }

    public ReminderRecord withAutoSuppressed(boolean nextAutoSuppressed) {
        return copy(reminderId, snoozeUntil, status, remindAt, timeZoneId, lastSyncedAt,
                reminderSource, sourceExpression, sourceSignature, naturalReferenceAt, nextAutoSuppressed);
    }

    public ReminderRecord withAutomaticMetadata(
            String nextSource,
            String nextExpression,
            String nextSignature,
            long nextNaturalReferenceAt,
            boolean nextAutoSuppressed) {
        return copy(reminderId, snoozeUntil, status, remindAt, timeZoneId, lastSyncedAt,
                nextSource, nextExpression, nextSignature, nextNaturalReferenceAt, nextAutoSuppressed);
    }

    private ReminderRecord copy(
            long nextReminderId,
            long nextSnoozeUntil,
            String nextStatus,
            long nextRemindAt,
            String nextTimeZoneId,
            long nextLastSyncedAt,
            String nextReminderSource,
            String nextSourceExpression,
            String nextSourceSignature,
            long nextNaturalReferenceAt,
            boolean nextAutoSuppressed) {
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
                nextTimeZoneId,
                nextReminderSource,
                nextSourceExpression,
                nextSourceSignature,
                nextNaturalReferenceAt,
                nextAutoSuppressed);
    }

    private static String defaultSource(String remindAtText, String remoteRemindAtText) {
        return safe(remoteRemindAtText).length() > 0 ? SOURCE_EXPLICIT : SOURCE_MANUAL;
    }

    private static String defaultExpression(String remindAtText, String remoteRemindAtText) {
        return safe(remoteRemindAtText).length() > 0 ? safe(remoteRemindAtText) : "";
    }

    private static String defaultSignature(String taskId, String remindAtText, String remoteRemindAtText) {
        String expression = safe(remoteRemindAtText).length() > 0 ? safe(remoteRemindAtText) : safe(remindAtText);
        return expression.length() == 0 ? "" : safe(taskId) + "|legacy|" + expression;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

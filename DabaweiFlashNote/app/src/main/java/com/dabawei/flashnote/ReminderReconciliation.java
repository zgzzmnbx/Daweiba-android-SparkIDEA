package com.dabawei.flashnote;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public final class ReminderReconciliation {
    private ReminderReconciliation() {
    }

    public static Plan plan(List<ReminderRecord> existing, List<TodoSyncItem> incoming, long nowMillis) {
        Map<String, ReminderRecord> existingByTaskId = new HashMap<>();
        if (existing != null) {
            for (ReminderRecord record : existing) {
                if (record != null && record.getTaskId().length() > 0) {
                    existingByTaskId.put(record.getTaskId(), record);
                }
            }
        }

        ArrayList<ReminderRecord> upserts = new ArrayList<>();
        ArrayList<ReminderRecord> cancellations = new ArrayList<>();
        Set<String> seenTaskIds = new HashSet<>();
        int overdueCount = 0;
        if (incoming != null) {
            for (TodoSyncItem item : incoming) {
                if (item == null) {
                    continue;
                }
                String taskId = item.getTaskId();
                if (taskId == null || taskId.trim().length() == 0) {
                    taskId = ReminderIds.generatedTaskId(item.getSourcePath(), item.getLineNumber(), item.getText());
                }
                taskId = taskId.trim();
                seenTaskIds.add(taskId);
                ReminderRecord old = existingByTaskId.get(taskId);
                long remindAt = TodoDateTime.parseDateTime(item.getRemindAtText());
                if (item.isDone() || remindAt <= 0L) {
                    if (old != null && !ReminderRecord.STATUS_CANCELLED.equals(old.getStatus())) {
                        cancellations.add(old.withStatus(ReminderRecord.STATUS_CANCELLED, 0L));
                    }
                    continue;
                }

                long dueAt = TodoDateTime.parseDue(item.getDueAtText());
                boolean remoteReminderUnchanged = old != null
                        && item.getRemindAtText().equals(old.getRemoteRemindAtText());
                boolean localReminderOverride = old != null
                        && !old.getRemindAtText().equals(old.getRemoteRemindAtText());
                if (remoteReminderUnchanged
                        && (localReminderOverride
                        || ReminderRecord.STATUS_CANCELLED.equals(old.getStatus())
                        || ReminderRecord.STATUS_FIRED.equals(old.getStatus())
                        || ReminderRecord.STATUS_OVERDUE.equals(old.getStatus()))) {
                    long preservedRemindAt = old.getRemindAt();
                    long preservedSnoozeUntil = old.getSnoozeUntil() > nowMillis
                            ? old.getSnoozeUntil()
                            : 0L;
                    String preservedStatus = old.getStatus();
                    if (ReminderRecord.STATUS_SCHEDULED.equals(preservedStatus)
                            || ReminderRecord.STATUS_SNOOZED.equals(preservedStatus)) {
                        if (preservedRemindAt <= nowMillis) {
                            preservedStatus = ReminderRecord.STATUS_OVERDUE;
                            preservedSnoozeUntil = 0L;
                            overdueCount++;
                        } else if (preservedSnoozeUntil > nowMillis) {
                            preservedStatus = ReminderRecord.STATUS_SNOOZED;
                        } else {
                            preservedStatus = ReminderRecord.STATUS_SCHEDULED;
                        }
                    }
                    upserts.add(new ReminderRecord(
                            old.getReminderId(),
                            taskId,
                            0L,
                            item.getText(),
                            item.getSourcePath(),
                            item.getBlockId(),
                            dueAt,
                            preservedRemindAt,
                            preservedSnoozeUntil,
                            preservedStatus,
                            old.getNotificationId(),
                            nowMillis,
                            item.getDueAtText(),
                            old.getRemindAtText(),
                            old.getRemoteRemindAtText(),
                            TimeZone.getDefault().getID()));
                    continue;
                }
                boolean unchanged = old != null
                        && old.getRemindAt() == remindAt
                        && old.getDueAt() == dueAt
                        && old.getTaskText().equals(item.getText())
                        && old.getSourcePath().equals(item.getSourcePath())
                        && old.getSourceBlockId().equals(item.getBlockId())
                        && old.getRemindAtText().equals(item.getRemindAtText());
                long snoozeUntil = unchanged && old.getSnoozeUntil() > nowMillis
                        ? old.getSnoozeUntil()
                        : 0L;
                String status;
                if (remindAt <= nowMillis) {
                    status = ReminderRecord.STATUS_OVERDUE;
                    overdueCount++;
                    snoozeUntil = 0L;
                } else if (snoozeUntil > nowMillis) {
                    status = ReminderRecord.STATUS_SNOOZED;
                } else {
                    status = ReminderRecord.STATUS_SCHEDULED;
                }

                int notificationId = old == null
                        ? ReminderIds.notificationIdForTaskId(taskId)
                        : old.getNotificationId();
                upserts.add(new ReminderRecord(
                        old == null ? 0L : old.getReminderId(),
                        taskId,
                        0L,
                        item.getText(),
                        item.getSourcePath(),
                        item.getBlockId(),
                        dueAt,
                        remindAt,
                        snoozeUntil,
                        status,
                        notificationId,
                        nowMillis,
                        item.getDueAtText(),
                        item.getRemindAtText(),
                        item.getRemindAtText(),
                        TimeZone.getDefault().getID()));
            }
        }

        for (ReminderRecord old : existingByTaskId.values()) {
            if (!seenTaskIds.contains(old.getTaskId())
                    && !ReminderRecord.STATUS_CANCELLED.equals(old.getStatus())) {
                cancellations.add(old.withStatus(ReminderRecord.STATUS_CANCELLED, 0L));
            }
        }
        return new Plan(upserts, cancellations, overdueCount);
    }

    public static final class Plan {
        private final List<ReminderRecord> upserts;
        private final List<ReminderRecord> cancellations;
        private final int overdueCount;

        private Plan(List<ReminderRecord> upserts, List<ReminderRecord> cancellations, int overdueCount) {
            this.upserts = upserts;
            this.cancellations = cancellations;
            this.overdueCount = overdueCount;
        }

        public List<ReminderRecord> getUpserts() {
            return upserts;
        }

        public List<ReminderRecord> getCancellations() {
            return cancellations;
        }

        public int getOverdueCount() {
            return overdueCount;
        }
    }
}

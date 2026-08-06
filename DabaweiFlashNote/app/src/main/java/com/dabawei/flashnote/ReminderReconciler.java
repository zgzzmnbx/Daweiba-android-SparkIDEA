package com.dabawei.flashnote;

import java.util.List;
import java.util.ArrayList;

public final class ReminderReconciler {
    private ReminderReconciler() {
    }

    public static Summary reconcile(
            FlashNoteDatabase database,
            List<TodoSyncItem> incoming,
            long nowMillis,
            ReminderScheduler scheduler) {
        ReminderReconciliation.Plan plan = ReminderReconciliation.plan(
                database.getRemoteReminders(),
                incoming,
                nowMillis);
        int scheduledCount = 0;
        for (ReminderRecord record : plan.getCancellations()) {
            scheduler.cancel(record);
            cancelOccurrences(database, scheduler, record.getTaskId());
            database.upsertReminder(record.withLastSyncedAt(nowMillis));
        }
        for (ReminderRecord record : plan.getUpserts()) {
            ReminderRecord previous = database.getReminderByTaskId(record.getTaskId());
            ReminderRecord synced = record.withLastSyncedAt(nowMillis);
            boolean needsScheduleUpdate = previous == null || scheduleChanged(previous, synced);
            if (previous != null && needsScheduleUpdate) {
                scheduler.cancel(previous);
                cancelOccurrences(database, scheduler, previous.getTaskId());
            }
            database.upsertReminder(synced);
            if (ReminderRecord.STATUS_SCHEDULED.equals(synced.getStatus())
                    || ReminderRecord.STATUS_SNOOZED.equals(synced.getStatus())) {
                if (needsScheduleUpdate) {
                    scheduler.schedule(synced);
                    scheduledCount++;
                }
            } else {
                scheduler.cancel(synced);
                cancelOccurrences(database, scheduler, synced.getTaskId());
            }
            scheduler.rescheduleOccurrencesForTask(synced.getTaskId());
        }
        return new Summary(
                scheduledCount,
                plan.getCancellations().size(),
                plan.getOverdueCount(),
                plan.getConflictTaskIds());
    }

    private static boolean scheduleChanged(ReminderRecord previous, ReminderRecord next) {
        return previous.getRemindAt() != next.getRemindAt()
                || !previous.getSourceSignature().equals(next.getSourceSignature())
                || !previous.getStatus().equals(next.getStatus())
                || previous.isAutoSuppressed() != next.isAutoSuppressed();
    }

    private static void cancelOccurrences(
            FlashNoteDatabase database,
            ReminderScheduler scheduler,
            String taskId) {
        for (ReminderOccurrence occurrence : database.getReminderOccurrencesForTask(taskId)) {
            scheduler.cancel(occurrence);
        }
        database.cancelReminderOccurrences(taskId);
    }

    public static final class Summary {
        private final int scheduledCount;
        private final int cancelledCount;
        private final int overdueCount;
        private final List<String> conflictTaskIds;

        private Summary(
                int scheduledCount,
                int cancelledCount,
                int overdueCount,
                List<String> conflictTaskIds) {
            this.scheduledCount = scheduledCount;
            this.cancelledCount = cancelledCount;
            this.overdueCount = overdueCount;
            this.conflictTaskIds = new ArrayList<>(conflictTaskIds == null
                    ? java.util.Collections.<String>emptyList()
                    : conflictTaskIds);
        }

        public int getScheduledCount() {
            return scheduledCount;
        }

        public int getCancelledCount() {
            return cancelledCount;
        }

        public int getOverdueCount() {
            return overdueCount;
        }

        public List<String> getConflictTaskIds() {
            return conflictTaskIds;
        }

        public int getConflictCount() {
            return conflictTaskIds.size();
        }
    }
}

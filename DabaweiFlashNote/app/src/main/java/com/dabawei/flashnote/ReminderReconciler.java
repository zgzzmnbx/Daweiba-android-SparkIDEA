package com.dabawei.flashnote;

import java.util.List;

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
            database.upsertReminder(record.withLastSyncedAt(nowMillis));
        }
        for (ReminderRecord record : plan.getUpserts()) {
            ReminderRecord synced = record.withLastSyncedAt(nowMillis);
            database.upsertReminder(synced);
            if (ReminderRecord.STATUS_SCHEDULED.equals(synced.getStatus())
                    || ReminderRecord.STATUS_SNOOZED.equals(synced.getStatus())) {
                scheduler.schedule(synced);
                scheduledCount++;
            } else {
                scheduler.cancel(synced);
            }
        }
        return new Summary(scheduledCount, plan.getCancellations().size(), plan.getOverdueCount());
    }

    public static final class Summary {
        private final int scheduledCount;
        private final int cancelledCount;
        private final int overdueCount;

        private Summary(int scheduledCount, int cancelledCount, int overdueCount) {
            this.scheduledCount = scheduledCount;
            this.cancelledCount = cancelledCount;
            this.overdueCount = overdueCount;
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
    }
}

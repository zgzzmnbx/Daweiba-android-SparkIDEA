package com.dabawei.flashnote;

import android.content.Context;

public final class TodoSyncCoordinator {
    private TodoSyncCoordinator() {
    }

    public static SyncResult sync(
            Context context,
            FlashNoteDatabase database,
            ReminderScheduler scheduler) {
        SyncSettings settings = SyncSettings.load(context);
        long now = System.currentTimeMillis();
        if (!settings.isReady()) {
            ReminderSettings.recordSyncFailure(context, now, "WebDAV 未配置");
            return SyncResult.failed("WebDAV 未配置");
        }
        WebDavTodoSyncReader.Result result = new WebDavTodoSyncReader().read(settings);
        if (!result.isSuccess()) {
            ReminderSettings.recordSyncFailure(context, now, result.getMessage());
            return SyncResult.failed(result.getMessage());
        }
        database.replaceRemoteTodos(result.getItems(), now);
        ReminderReconciler.Summary summary = ReminderReconciler.reconcile(
                database,
                result.getItems(),
                now,
                scheduler);
        ReminderSettings.recordSyncSuccess(context, now, result.getItems().size());
        return SyncResult.success(result.getItems(), summary);
    }

    public static SyncResult syncInNewDatabase(Context context) {
        FlashNoteDatabase database = new FlashNoteDatabase(context);
        try {
            return sync(context, database, new ReminderScheduler(context, database));
        } finally {
            database.close();
        }
    }

    public static final class SyncResult {
        private final boolean success;
        private final java.util.List<TodoSyncItem> items;
        private final ReminderReconciler.Summary summary;
        private final String message;

        private SyncResult(
                boolean success,
                java.util.List<TodoSyncItem> items,
                ReminderReconciler.Summary summary,
                String message) {
            this.success = success;
            this.items = items;
            this.summary = summary;
            this.message = message;
        }

        static SyncResult success(
                java.util.List<TodoSyncItem> items,
                ReminderReconciler.Summary summary) {
            return new SyncResult(true, items, summary, "Synced");
        }

        static SyncResult failed(String message) {
            return new SyncResult(false, null, null, message == null ? "同步失败" : message);
        }

        public boolean isSuccess() {
            return success;
        }

        public java.util.List<TodoSyncItem> getItems() {
            return items;
        }

        public ReminderReconciler.Summary getSummary() {
            return summary;
        }

        public String getMessage() {
            return message;
        }
    }
}

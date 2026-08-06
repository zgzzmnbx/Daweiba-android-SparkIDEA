package com.dabawei.flashnote;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public final class FlashNoteDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "dabawei_flash_notes.db";
    private static final int DATABASE_VERSION = 8;
    private static final String TABLE_NOTES = "flash_notes";
    private static final String TABLE_REMINDERS = "reminders";
    private static final String TABLE_TODO_ITEMS = "todo_items";
    private static final String TABLE_OCCURRENCES = "reminder_occurrences";
    public static final int SYNC_PENDING = 0;
    public static final int SYNC_SYNCED = 1;
    public static final int SYNC_FAILED = 2;

    public FlashNoteDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NOTES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "content TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL, " +
                "sync_state INTEGER NOT NULL DEFAULT 0, " +
                "note_type INTEGER NOT NULL DEFAULT 0" +
                ")");
        createRemindersTable(db);
        createTodoItemsTable(db);
        createOccurrencesTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN sync_state INTEGER NOT NULL DEFAULT 0");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN note_type INTEGER NOT NULL DEFAULT 0");
        }
        if (oldVersion < 4) {
            createRemindersTable(db);
        }
        if (oldVersion >= 4 && oldVersion < 5) {
            db.execSQL("ALTER TABLE " + TABLE_REMINDERS
                    + " ADD COLUMN remote_remind_at_text TEXT NOT NULL DEFAULT ''");
        }
        if (oldVersion < 6) {
            createTodoItemsTable(db);
        }
        if (oldVersion < 7) {
            createOccurrencesTable(db);
        }
        if (oldVersion >= 4 && oldVersion < 8) {
            db.execSQL("ALTER TABLE " + TABLE_REMINDERS
                    + " ADD COLUMN reminder_source TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE " + TABLE_REMINDERS
                    + " ADD COLUMN source_expression TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE " + TABLE_REMINDERS
                    + " ADD COLUMN source_signature TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE " + TABLE_REMINDERS
                    + " ADD COLUMN natural_reference_at INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_REMINDERS
                    + " ADD COLUMN auto_suppressed INTEGER NOT NULL DEFAULT 0");
        }
    }

    public long insertNote(String content, long createdAtMillis) {
        return insertNote(content, createdAtMillis, FlashNote.TYPE_NOTE);
    }

    public long insertNote(String content, long createdAtMillis, int noteType) {
        ContentValues values = new ContentValues();
        values.put("content", content);
        values.put("created_at", createdAtMillis);
        values.put("sync_state", SYNC_PENDING);
        values.put("note_type", noteType);
        return getWritableDatabase().insertOrThrow(TABLE_NOTES, null, values);
    }

    public void markSyncState(long id, int state) {
        ContentValues values = new ContentValues();
        values.put("sync_state", state);
        getWritableDatabase().update(TABLE_NOTES, values, "id = ?", new String[]{String.valueOf(id)});
    }

    public void updateNoteContent(long id, String content) {
        ContentValues values = new ContentValues();
        values.put("content", content);
        values.put("sync_state", SYNC_PENDING);
        getWritableDatabase().update(TABLE_NOTES, values, "id = ?", new String[]{String.valueOf(id)});
    }

    public void updateNoteType(long id, int noteType) {
        ContentValues values = new ContentValues();
        values.put("note_type", noteType);
        values.put("sync_state", SYNC_PENDING);
        getWritableDatabase().update(TABLE_NOTES, values, "id = ?", new String[]{String.valueOf(id)});
    }

    public void markSyncState(List<FlashNote> notes, int state) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("sync_state", state);
            for (FlashNote note : notes) {
                db.update(TABLE_NOTES, values, "id = ?", new String[]{String.valueOf(note.getId())});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void deleteNote(long id) {
        getWritableDatabase().delete(TABLE_NOTES, "id = ?", new String[]{String.valueOf(id)});
    }

    public void upsertReminder(ReminderRecord reminder) {
        if (reminder == null || reminder.getTaskId().length() == 0) {
            return;
        }
        ContentValues values = reminderValues(reminder);
        SQLiteDatabase db = getWritableDatabase();
        int updated = db.update(
                TABLE_REMINDERS,
                values,
                "task_id = ?",
                new String[]{reminder.getTaskId()});
        if (updated == 0) {
            db.insertOrThrow(TABLE_REMINDERS, null, values);
        }
    }

    public ReminderRecord getReminderByTaskId(String taskId) {
        if (taskId == null || taskId.trim().length() == 0) {
            return null;
        }
        List<ReminderRecord> records = queryReminders(
                "task_id = ?",
                new String[]{taskId.trim()},
                "reminder_id DESC");
        return records.isEmpty() ? null : records.get(0);
    }

    public ReminderRecord getReminderForLocalNote(long localNoteId) {
        if (localNoteId <= 0L) {
            return null;
        }
        List<ReminderRecord> records = queryReminders(
                "local_note_id = ?",
                new String[]{String.valueOf(localNoteId)},
                "reminder_id DESC");
        return records.isEmpty() ? null : records.get(0);
    }

    public List<ReminderRecord> getRemoteReminders() {
        return queryReminders("local_note_id = 0", null, "reminder_id ASC");
    }

    public List<ReminderRecord> getSchedulableReminders() {
        return queryReminders(
                "status = ? OR status = ?",
                new String[]{ReminderRecord.STATUS_SCHEDULED, ReminderRecord.STATUS_SNOOZED},
                "remind_at ASC, reminder_id ASC");
    }

    public void replaceRemoteTodos(List<TodoSyncItem> items, long syncedAt) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_TODO_ITEMS, null, null);
            if (items != null) {
                for (TodoSyncItem item : items) {
                    if (item == null || item.isDone() || item.getTaskId().length() == 0) {
                        continue;
                    }
                    ContentValues values = new ContentValues();
                    values.put("task_id", item.getTaskId());
                    values.put("task_text", item.getText());
                    values.put("source_path", item.getSourcePath());
                    values.put("source_block_id", item.getBlockId());
                    values.put("due_at", TodoDateTime.parseDue(item.getDueAtText()));
                    values.put("remind_at", TodoDateTime.parseDateTime(item.getRemindAtText()));
                    values.put("due_at_text", item.getDueAtText());
                    values.put("remind_at_text", item.getRemindAtText());
                    values.put("last_synced_at", syncedAt);
                    db.insertWithOnConflict(TABLE_TODO_ITEMS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<TodoSyncItem> getOverviewTodos(long endOfToday) {
        List<TodoSyncItem> items = new ArrayList<>();
        Cursor cursor = getReadableDatabase().query(
                TABLE_TODO_ITEMS,
                new String[]{
                        "task_id", "task_text", "source_path", "source_block_id",
                        "due_at_text", "remind_at_text"
                },
                "done = 0 AND ((due_at > 0 AND due_at <= ?) OR (remind_at > 0 AND remind_at <= ?))",
                new String[]{String.valueOf(endOfToday), String.valueOf(endOfToday)},
                null,
                null,
                "due_at ASC, remind_at ASC, task_id ASC");
        try {
            while (cursor.moveToNext()) {
                items.add(new TodoSyncItem(
                        cursor.getString(1),
                        false,
                        cursor.getString(0),
                        cursor.getString(2),
                        0,
                        cursor.getString(3),
                        "",
                        cursor.getString(4),
                        cursor.getString(5)));
            }
        } finally {
            cursor.close();
        }
        return items;
    }

    public void upsertReminderOccurrence(ReminderOccurrence occurrence) {
        if (occurrence == null || occurrence.getTaskId().length() == 0 || occurrence.getKind().length() == 0) {
            return;
        }
        ContentValues values = occurrenceValues(occurrence);
        SQLiteDatabase db = getWritableDatabase();
        int updated = db.update(
                TABLE_OCCURRENCES,
                values,
                "task_id = ? AND kind = ?",
                new String[]{occurrence.getTaskId(), occurrence.getKind()});
        if (updated == 0) {
            db.insertOrThrow(TABLE_OCCURRENCES, null, values);
        }
    }

    public ReminderOccurrence getReminderOccurrenceById(long occurrenceId) {
        if (occurrenceId <= 0L) {
            return null;
        }
        List<ReminderOccurrence> occurrences = queryOccurrences(
                "occurrence_id = ?",
                new String[]{String.valueOf(occurrenceId)},
                "occurrence_id DESC");
        return occurrences.isEmpty() ? null : occurrences.get(0);
    }

    public List<ReminderOccurrence> getReminderOccurrencesForTask(String taskId) {
        if (taskId == null || taskId.trim().length() == 0) {
            return new ArrayList<>();
        }
        return queryOccurrences(
                "task_id = ?",
                new String[]{taskId.trim()},
                "occurrence_id ASC");
    }

    public List<ReminderOccurrence> getSchedulableOccurrences() {
        return queryOccurrences(
                "status = ? OR status = ?",
                new String[]{ReminderRecord.STATUS_SCHEDULED, ReminderRecord.STATUS_SNOOZED},
                "trigger_at ASC, occurrence_id ASC");
    }

    public int getScheduledReminderCount() {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT (SELECT COUNT(*) FROM " + TABLE_REMINDERS
                        + " WHERE status IN (?, ?)) + (SELECT COUNT(*) FROM " + TABLE_OCCURRENCES
                        + " WHERE status IN (?, ?))",
                new String[]{
                        ReminderRecord.STATUS_SCHEDULED,
                        ReminderRecord.STATUS_SNOOZED,
                        ReminderRecord.STATUS_SCHEDULED,
                        ReminderRecord.STATUS_SNOOZED
                });
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    private void cancelOccurrencesForTaskInternal(SQLiteDatabase db, String taskId) {
        ContentValues values = new ContentValues();
        values.put("status", ReminderRecord.STATUS_CANCELLED);
        values.put("snooze_until", 0L);
        db.update(TABLE_OCCURRENCES, values, "task_id = ?", new String[]{taskId});
    }

    public void cancelReminderOccurrences(String taskId) {
        if (taskId == null || taskId.trim().length() == 0) {
            return;
        }
        cancelOccurrencesForTaskInternal(getWritableDatabase(), taskId.trim());
    }

    private void createRemindersTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_REMINDERS + " (" +
                "reminder_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "task_id TEXT NOT NULL UNIQUE, " +
                "local_note_id INTEGER NOT NULL DEFAULT 0, " +
                "task_text TEXT NOT NULL DEFAULT '', " +
                "source_path TEXT NOT NULL DEFAULT '', " +
                "source_block_id TEXT NOT NULL DEFAULT '', " +
                "due_at INTEGER NOT NULL DEFAULT 0, " +
                "remind_at INTEGER NOT NULL DEFAULT 0, " +
                "snooze_until INTEGER NOT NULL DEFAULT 0, " +
                "status TEXT NOT NULL, " +
                "notification_id INTEGER NOT NULL, " +
                "last_synced_at INTEGER NOT NULL DEFAULT 0, " +
                "due_at_text TEXT NOT NULL DEFAULT '', " +
                "remind_at_text TEXT NOT NULL DEFAULT '', " +
                "remote_remind_at_text TEXT NOT NULL DEFAULT '', " +
                "time_zone_id TEXT NOT NULL DEFAULT '', " +
                "reminder_source TEXT NOT NULL DEFAULT '', " +
                "source_expression TEXT NOT NULL DEFAULT '', " +
                "source_signature TEXT NOT NULL DEFAULT '', " +
                "natural_reference_at INTEGER NOT NULL DEFAULT 0, " +
                "auto_suppressed INTEGER NOT NULL DEFAULT 0" +
                ")");
    }

    private void createTodoItemsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_TODO_ITEMS + " (" +
                "task_id TEXT PRIMARY KEY, " +
                "task_text TEXT NOT NULL DEFAULT '', " +
                "done INTEGER NOT NULL DEFAULT 0, " +
                "source_path TEXT NOT NULL DEFAULT '', " +
                "source_block_id TEXT NOT NULL DEFAULT '', " +
                "due_at INTEGER NOT NULL DEFAULT 0, " +
                "remind_at INTEGER NOT NULL DEFAULT 0, " +
                "due_at_text TEXT NOT NULL DEFAULT '', " +
                "remind_at_text TEXT NOT NULL DEFAULT '', " +
                "last_synced_at INTEGER NOT NULL DEFAULT 0" +
                ")");
    }

    private void createOccurrencesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_OCCURRENCES + " (" +
                "occurrence_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "task_id TEXT NOT NULL, " +
                "kind TEXT NOT NULL, " +
                "trigger_at INTEGER NOT NULL DEFAULT 0, " +
                "snooze_until INTEGER NOT NULL DEFAULT 0, " +
                "status TEXT NOT NULL, " +
                "notification_id INTEGER NOT NULL, " +
                "last_synced_at INTEGER NOT NULL DEFAULT 0, " +
                "time_zone_id TEXT NOT NULL DEFAULT '', " +
                "UNIQUE(task_id, kind)" +
                ")");
    }

    private ContentValues reminderValues(ReminderRecord reminder) {
        ContentValues values = new ContentValues();
        values.put("task_id", reminder.getTaskId());
        values.put("local_note_id", reminder.getLocalNoteId());
        values.put("task_text", reminder.getTaskText());
        values.put("source_path", reminder.getSourcePath());
        values.put("source_block_id", reminder.getSourceBlockId());
        values.put("due_at", reminder.getDueAt());
        values.put("remind_at", reminder.getRemindAt());
        values.put("snooze_until", reminder.getSnoozeUntil());
        values.put("status", reminder.getStatus());
        values.put("notification_id", reminder.getNotificationId());
        values.put("last_synced_at", reminder.getLastSyncedAt());
        values.put("due_at_text", reminder.getDueAtText());
        values.put("remind_at_text", reminder.getRemindAtText());
        values.put("remote_remind_at_text", reminder.getRemoteRemindAtText());
        values.put("time_zone_id", reminder.getTimeZoneId());
        values.put("reminder_source", reminder.getReminderSource());
        values.put("source_expression", reminder.getSourceExpression());
        values.put("source_signature", reminder.getSourceSignature());
        values.put("natural_reference_at", reminder.getNaturalReferenceAt());
        values.put("auto_suppressed", reminder.isAutoSuppressed() ? 1 : 0);
        return values;
    }

    private ContentValues occurrenceValues(ReminderOccurrence occurrence) {
        ContentValues values = new ContentValues();
        values.put("task_id", occurrence.getTaskId());
        values.put("kind", occurrence.getKind());
        values.put("trigger_at", occurrence.getTriggerAt());
        values.put("snooze_until", occurrence.getSnoozeUntil());
        values.put("status", occurrence.getStatus());
        values.put("notification_id", occurrence.getNotificationId());
        values.put("last_synced_at", occurrence.getLastSyncedAt());
        values.put("time_zone_id", occurrence.getTimeZoneId());
        return values;
    }

    private List<ReminderRecord> queryReminders(String selection, String[] selectionArgs, String orderBy) {
        List<ReminderRecord> records = new ArrayList<>();
        Cursor cursor = getReadableDatabase().query(
                TABLE_REMINDERS,
                new String[]{
                        "reminder_id", "task_id", "local_note_id", "task_text", "source_path",
                        "source_block_id", "due_at", "remind_at", "snooze_until", "status",
                        "notification_id", "last_synced_at", "due_at_text", "remind_at_text",
                        "remote_remind_at_text", "time_zone_id", "reminder_source",
                        "source_expression", "source_signature", "natural_reference_at", "auto_suppressed"
                },
                selection,
                selectionArgs,
                null,
                null,
                orderBy);
        try {
            while (cursor.moveToNext()) {
                records.add(new ReminderRecord(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getLong(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.getLong(6),
                        cursor.getLong(7),
                        cursor.getLong(8),
                        cursor.getString(9),
                        cursor.getInt(10),
                        cursor.getLong(11),
                        cursor.getString(12),
                        cursor.getString(13),
                        cursor.getString(14),
                        cursor.getString(15),
                        cursor.getString(16),
                        cursor.getString(17),
                        cursor.getString(18),
                        cursor.getLong(19),
                        cursor.getInt(20) != 0));
            }
        } finally {
            cursor.close();
        }
        return records;
    }

    private List<ReminderOccurrence> queryOccurrences(String selection, String[] selectionArgs, String orderBy) {
        List<ReminderOccurrence> occurrences = new ArrayList<>();
        Cursor cursor = getReadableDatabase().query(
                TABLE_OCCURRENCES,
                new String[]{
                        "occurrence_id", "task_id", "kind", "trigger_at", "snooze_until",
                        "status", "notification_id", "last_synced_at", "time_zone_id"
                },
                selection,
                selectionArgs,
                null,
                null,
                orderBy);
        try {
            while (cursor.moveToNext()) {
                occurrences.add(new ReminderOccurrence(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getLong(3),
                        cursor.getLong(4),
                        cursor.getString(5),
                        cursor.getInt(6),
                        cursor.getLong(7),
                        cursor.getString(8)));
            }
        } finally {
            cursor.close();
        }
        return occurrences;
    }

    public List<FlashNote> getRecentNotes() {
        return queryNotes(null, null);
    }

    public List<FlashNote> getPendingNotes() {
        return queryNotes("sync_state != ?", new String[]{String.valueOf(SYNC_SYNCED)});
    }

    public List<FlashNote> searchNotes(String keyword) {
        String trimmed = keyword == null ? "" : keyword.trim();
        if (trimmed.length() == 0) {
            return getRecentNotes();
        }
        return queryNotes("content LIKE ?", new String[]{"%" + trimmed + "%"});
    }

    private List<FlashNote> queryNotes(String selection, String[] selectionArgs) {
        List<FlashNote> notes = new ArrayList<>();
        Cursor cursor = getReadableDatabase().query(
                TABLE_NOTES,
                new String[]{"id", "content", "created_at", "sync_state", "note_type"},
                selection,
                selectionArgs,
                null,
                null,
                "created_at DESC, id DESC");
        try {
            while (cursor.moveToNext()) {
                notes.add(new FlashNote(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getLong(2),
                        cursor.getInt(3),
                        cursor.getInt(4)));
            }
        } finally {
            cursor.close();
        }
        return notes;
    }
}

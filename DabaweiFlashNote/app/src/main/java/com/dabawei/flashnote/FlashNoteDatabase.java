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
    private static final int DATABASE_VERSION = 5;
    private static final String TABLE_NOTES = "flash_notes";
    private static final String TABLE_REMINDERS = "reminders";
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
                "time_zone_id TEXT NOT NULL DEFAULT ''" +
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
                        "remote_remind_at_text", "time_zone_id"
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
                        cursor.getString(15)));
            }
        } finally {
            cursor.close();
        }
        return records;
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

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
    private static final int DATABASE_VERSION = 3;
    private static final String TABLE_NOTES = "flash_notes";
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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN sync_state INTEGER NOT NULL DEFAULT 0");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN note_type INTEGER NOT NULL DEFAULT 0");
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

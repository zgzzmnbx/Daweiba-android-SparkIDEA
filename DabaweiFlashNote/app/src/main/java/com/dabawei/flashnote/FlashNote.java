package com.dabawei.flashnote;

public final class FlashNote {
    public static final int TYPE_NOTE = 0;
    public static final int TYPE_TODO = 1;

    private final long id;
    private final String content;
    private final long createdAtMillis;
    private final int syncState;
    private final int noteType;

    public FlashNote(long id, String content, long createdAtMillis) {
        this(id, content, createdAtMillis, 0, TYPE_NOTE);
    }

    public FlashNote(long id, String content, long createdAtMillis, int syncState) {
        this(id, content, createdAtMillis, syncState, TYPE_NOTE);
    }

    public FlashNote(long id, String content, long createdAtMillis, int syncState, int noteType) {
        this.id = id;
        this.content = content;
        this.createdAtMillis = createdAtMillis;
        this.syncState = syncState;
        this.noteType = noteType;
    }

    public long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public int getSyncState() {
        return syncState;
    }

    public int getNoteType() {
        return noteType;
    }
}

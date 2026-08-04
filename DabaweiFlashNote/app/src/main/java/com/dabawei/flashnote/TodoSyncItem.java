package com.dabawei.flashnote;

public final class TodoSyncItem {
    private final String text;
    private final boolean done;
    private final String sourcePath;
    private final int lineNumber;
    private final String blockId;
    private final String note;

    public TodoSyncItem(String text, boolean done, String sourcePath, int lineNumber, String blockId, String note) {
        this.text = text == null ? "" : text;
        this.done = done;
        this.sourcePath = sourcePath == null ? "" : sourcePath;
        this.lineNumber = lineNumber;
        this.blockId = blockId == null ? "" : blockId;
        this.note = note == null ? "" : note;
    }

    public String getText() {
        return text;
    }

    public boolean isDone() {
        return done;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getBlockId() {
        return blockId;
    }

    public String getNote() {
        return note;
    }
}

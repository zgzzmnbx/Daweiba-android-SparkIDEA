package com.dabawei.flashnote;

public final class TodoSyncItem {
    private final String text;
    private final boolean done;
    private final String taskId;
    private final String sourcePath;
    private final int lineNumber;
    private final String blockId;
    private final String note;
    private final String dueAtText;
    private final String remindAtText;

    public TodoSyncItem(String text, boolean done, String sourcePath, int lineNumber, String blockId, String note) {
        this(text, done, blockId, sourcePath, lineNumber, blockId, note, "", "");
    }

    public TodoSyncItem(
            String text,
            boolean done,
            String taskId,
            String sourcePath,
            int lineNumber,
            String blockId,
            String note,
            String dueAtText,
            String remindAtText) {
        this.text = text == null ? "" : text;
        this.done = done;
        this.taskId = taskId == null ? "" : taskId;
        this.sourcePath = sourcePath == null ? "" : sourcePath;
        this.lineNumber = lineNumber;
        this.blockId = blockId == null ? "" : blockId;
        this.note = note == null ? "" : note;
        this.dueAtText = dueAtText == null ? "" : dueAtText;
        this.remindAtText = remindAtText == null ? "" : remindAtText;
    }

    public String getText() {
        return text;
    }

    public boolean isDone() {
        return done;
    }

    public String getTaskId() {
        return taskId;
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

    public String getDueAtText() {
        return dueAtText;
    }

    public String getRemindAtText() {
        return remindAtText;
    }
}

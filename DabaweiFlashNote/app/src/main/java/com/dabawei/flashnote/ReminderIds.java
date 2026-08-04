package com.dabawei.flashnote;

import java.util.Locale;

public final class ReminderIds {
    private ReminderIds() {
    }

    public static String localTaskId(long noteId) {
        return "local-note-" + noteId;
    }

    public static int notificationIdForTaskId(String taskId) {
        long hash = fnv1a(taskId == null ? "" : taskId);
        int notificationId = (int) (hash & 0x7fffffffL);
        return notificationId == 0 ? 1 : notificationId;
    }

    public static String generatedTaskId(String sourcePath, int lineNumber, String text) {
        String seed = (sourcePath == null ? "" : sourcePath)
                + "|" + lineNumber
                + "|" + (text == null ? "" : text);
        return "todo-" + String.format(Locale.US, "%08x", fnv1a(seed));
    }

    private static long fnv1a(String value) {
        long hash = 2166136261L;
        for (int index = 0; index < value.length(); index++) {
            hash ^= value.charAt(index);
            hash = (hash * 16777619L) & 0xffffffffL;
        }
        return hash;
    }
}

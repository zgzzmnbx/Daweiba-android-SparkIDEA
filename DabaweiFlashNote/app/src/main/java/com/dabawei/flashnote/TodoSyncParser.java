package com.dabawei.flashnote;

import java.util.ArrayList;
import java.util.List;

public final class TodoSyncParser {
    public static final String BEGIN_MARKER = "<!-- DABAWEI_TODO_SYNC_BEGIN -->";
    public static final String END_MARKER = "<!-- DABAWEI_TODO_SYNC_END -->";

    private TodoSyncParser() {
    }

    public static List<TodoSyncItem> parse(String markdown) {
        ArrayList<TodoSyncItem> items = new ArrayList<>();
        if (markdown == null || markdown.length() == 0) {
            return items;
        }

        String[] lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        boolean inBlock = false;
        Builder current = null;
        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine;
            String trimmed = line.trim();
            if (BEGIN_MARKER.equals(trimmed)) {
                inBlock = true;
                current = null;
                continue;
            }
            if (END_MARKER.equals(trimmed)) {
                addIfPresent(items, current);
                break;
            }
            if (!inBlock) {
                continue;
            }
            if (isTaskLine(trimmed)) {
                addIfPresent(items, current);
                current = Builder.fromTaskLine(trimmed);
            } else if (current != null) {
                current.readMetadata(trimmed);
            }
        }
        return items;
    }

    private static boolean isTaskLine(String line) {
        return line.startsWith("- [ ] ") || line.startsWith("- [x] ") || line.startsWith("- [X] ");
    }

    private static void addIfPresent(ArrayList<TodoSyncItem> items, Builder current) {
        if (current != null) {
            items.add(current.build());
        }
    }

    private static final class Builder {
        private String text = "";
        private boolean done;
        private String sourcePath = "";
        private int lineNumber;
        private String blockId = "";
        private String note = "";

        static Builder fromTaskLine(String line) {
            Builder builder = new Builder();
            builder.done = line.startsWith("- [x] ") || line.startsWith("- [X] ");
            builder.text = line.substring(6).trim();
            return builder;
        }

        void readMetadata(String line) {
            if (line.startsWith("来源文件::")) {
                sourcePath = afterDoubleColon(line);
            } else if (line.startsWith("行号::")) {
                lineNumber = parseInt(afterDoubleColon(line));
            } else if (line.startsWith("块ID::")) {
                blockId = afterDoubleColon(line);
            } else if (line.startsWith("备注::")) {
                note = afterDoubleColon(line);
            }
        }

        TodoSyncItem build() {
            return new TodoSyncItem(text, done, sourcePath, lineNumber, blockId, note);
        }

        private static String afterDoubleColon(String line) {
            int index = line.indexOf("::");
            return index >= 0 ? line.substring(index + 2).trim() : "";
        }

        private static int parseInt(String value) {
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
                return 0;
            }
        }
    }
}

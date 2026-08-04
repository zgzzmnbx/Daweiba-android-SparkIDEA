package com.dabawei.flashnote;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class MarkdownAnchorInserter {
    public static final String DEFAULT_ANCHOR = "<!-- DABAWEI_FLASHNOTE_INBOX -->";

    private MarkdownAnchorInserter() {
    }

    public static Result insertBelowAnchor(String markdown, String anchor, String noteLine) {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        lines.add(noteLine);
        return insertLinesBelowAnchor(markdown, anchor, lines);
    }

    public static Result insertLinesBelowAnchor(String markdown, String anchor, List<String> noteLines) {
        String safeMarkdown = markdown == null ? "" : markdown;
        String safeAnchor = anchor == null || anchor.trim().length() == 0 ? DEFAULT_ANCHOR : anchor.trim();
        int anchorIndex = safeMarkdown.indexOf(safeAnchor);
        if (anchorIndex < 0) {
            return Result.missingAnchor(safeMarkdown, "Missing anchor: " + safeAnchor);
        }

        int anchorEnd = anchorIndex + safeAnchor.length();
        String lineBreak = detectLineBreak(safeMarkdown);
        String insertion = joinLines(noteLines, lineBreak);
        if (insertion.length() == 0) {
            return Result.inserted(safeMarkdown);
        }
        int insertAt = anchorEnd;
        if (safeMarkdown.startsWith("\r\n", anchorEnd)) {
            insertAt = anchorEnd + 2;
        } else if (safeMarkdown.startsWith("\n", anchorEnd) || safeMarkdown.startsWith("\r", anchorEnd)) {
            insertAt = anchorEnd + 1;
        } else {
            return Result.inserted(
                    safeMarkdown.substring(0, anchorEnd)
                            + lineBreak
                            + insertion
                            + lineBreak
                            + safeMarkdown.substring(anchorEnd));
        }

        return Result.inserted(
                safeMarkdown.substring(0, insertAt)
                        + insertion
                        + lineBreak
                        + safeMarkdown.substring(insertAt));
    }

    private static String joinLines(List<String> noteLines, String lineBreak) {
        if (noteLines == null || noteLines.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String line : noteLines) {
            String safeLine = line == null ? "" : line.trim();
            if (safeLine.length() == 0) {
                continue;
            }
            if (builder.length() > 0) {
                boolean separateBlocks = builder.indexOf(lineBreak) >= 0 || safeLine.indexOf(lineBreak) >= 0;
                builder.append(lineBreak);
                if (separateBlocks) {
                    builder.append(lineBreak);
                }
            }
            builder.append(safeLine);
        }
        return builder.toString();
    }

    public static String formatFlashNoteLine(String content, long createdAtMillis, TimeZone timeZone) {
        return formatNoteLine(content, createdAtMillis, timeZone, FlashNote.TYPE_NOTE);
    }

    public static String formatNoteLine(String content, long createdAtMillis, TimeZone timeZone, int noteType) {
        return formatNoteLine(content, createdAtMillis, timeZone, noteType, "", "");
    }

    public static String formatNoteLine(
            String content,
            long createdAtMillis,
            TimeZone timeZone,
            int noteType,
            String taskId,
            String remindAtText) {
        return formatNoteLine(content, createdAtMillis, timeZone, noteType, taskId, "", remindAtText);
    }

    public static String formatNoteLine(
            String content,
            long createdAtMillis,
            TimeZone timeZone,
            int noteType,
            String taskId,
            String dueAtText,
            String remindAtText) {
        TimeZone safeTimeZone = timeZone == null ? TimeZone.getDefault() : timeZone;
        SimpleDateFormat titleFormat = new SimpleDateFormat("yyyyMMddHHmm", Locale.US);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        SimpleDateFormat blockFormat = new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US);
        titleFormat.setTimeZone(safeTimeZone);
        dateFormat.setTimeZone(safeTimeZone);
        blockFormat.setTimeZone(safeTimeZone);
        Date createdAt = new Date(createdAtMillis);
        String safeContent = content == null ? "" : content.trim().replace("\r", "").replace("\n", " / ");
        String dateText = dateFormat.format(createdAt);
        String titleText = titleFormat.format(createdAt);
        String blockText = blockFormat.format(createdAt);
        StringBuilder builder = new StringBuilder();
        builder.append("**大尾巴闪念-").append(titleText).append("**").append("\n");
        if (noteType == FlashNote.TYPE_TODO) {
            builder.append("- [ ] ").append(safeContent).append(" #闪念 #待办").append("\n");
        } else {
            builder.append("- ").append(safeContent).append(" #闪念").append("\n");
        }
        builder.append("  记录日期:: ").append(dateText).append("\n");
        if (noteType == FlashNote.TYPE_TODO && taskId != null && taskId.trim().length() > 0) {
            builder.append("  任务ID:: ").append(taskId.trim()).append("\n");
        }
        if (noteType == FlashNote.TYPE_TODO) {
            builder.append("  截止日期:: ")
                    .append(dueAtText == null ? "" : dueAtText.trim())
                    .append("\n");
            builder.append("  提醒时间:: ")
                    .append(remindAtText == null ? "" : remindAtText.trim())
                    .append("\n");
        }
        builder.append("  备注::").append("\n");
        builder.append("  ^flash-").append(blockText);
        return builder.toString();
    }

    private static String detectLineBreak(String markdown) {
        return markdown != null && markdown.contains("\r\n") ? "\r\n" : "\n";
    }

    public static final class Result {
        private final boolean inserted;
        private final String markdown;
        private final String errorMessage;

        private Result(boolean inserted, String markdown, String errorMessage) {
            this.inserted = inserted;
            this.markdown = markdown;
            this.errorMessage = errorMessage;
        }

        static Result inserted(String markdown) {
            return new Result(true, markdown, null);
        }

        static Result missingAnchor(String markdown, String errorMessage) {
            return new Result(false, markdown, errorMessage);
        }

        public boolean isInserted() {
            return inserted;
        }

        public String getMarkdown() {
            return markdown;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}

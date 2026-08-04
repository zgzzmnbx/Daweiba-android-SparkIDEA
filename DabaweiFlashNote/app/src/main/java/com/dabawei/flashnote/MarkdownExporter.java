package com.dabawei.flashnote;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class MarkdownExporter {
    private MarkdownExporter() {
    }

    public static String toMarkdown(List<FlashNote> notes, TimeZone timeZone) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
        dateFormat.setTimeZone(timeZone);
        timeFormat.setTimeZone(timeZone);

        StringBuilder builder = new StringBuilder();
        builder.append("# 大尾巴闪念导出\n");

        String currentDate = null;
        for (FlashNote note : notes) {
            String content = note.getContent() == null ? "" : note.getContent().trim();
            if (content.length() == 0) {
                continue;
            }

            Date createdAt = new Date(note.getCreatedAtMillis());
            String noteDate = dateFormat.format(createdAt);
            if (!noteDate.equals(currentDate)) {
                builder.append("\n## ").append(noteDate).append("\n\n");
                currentDate = noteDate;
            }

            builder.append("- ")
                    .append(timeFormat.format(createdAt))
                    .append(" ")
                    .append(content.replace("\r", "").replace("\n", " / "))
                    .append("\n");
        }

        return builder.toString();
    }
}

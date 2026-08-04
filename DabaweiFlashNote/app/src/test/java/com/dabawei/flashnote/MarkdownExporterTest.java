package com.dabawei.flashnote;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public final class MarkdownExporterTest {
    public static void main(String[] args) {
        exportsNotesGroupedByDate();
        ignoresBlankNotes();
        System.out.println("MarkdownExporter tests passed.");
    }

    private static void exportsNotesGroupedByDate() {
        List<FlashNote> notes = new ArrayList<>();
        notes.add(new FlashNote(1L, "打开即写", utcMillis(2026, 6, 6, 9, 32)));
        notes.add(new FlashNote(2L, "一键保存", utcMillis(2026, 6, 6, 10, 15)));
        notes.add(new FlashNote(3L, "第二天想法", utcMillis(2026, 6, 7, 8, 0)));

        String markdown = MarkdownExporter.toMarkdown(notes, TimeZone.getTimeZone("UTC"));

        assertContains(markdown, "# 大尾巴闪念导出");
        assertContains(markdown, "## 2026-06-06");
        assertContains(markdown, "- 09:32 打开即写");
        assertContains(markdown, "- 10:15 一键保存");
        assertContains(markdown, "## 2026-06-07");
        assertContains(markdown, "- 08:00 第二天想法");
    }

    private static void ignoresBlankNotes() {
        List<FlashNote> notes = new ArrayList<>();
        notes.add(new FlashNote(1L, "   ", utcMillis(2026, 6, 6, 9, 32)));
        notes.add(new FlashNote(2L, "留下真正内容", utcMillis(2026, 6, 6, 10, 15)));

        String markdown = MarkdownExporter.toMarkdown(notes, TimeZone.getTimeZone("UTC"));

        assertNotContains(markdown, "- 09:32");
        assertContains(markdown, "- 10:15 留下真正内容");
    }

    private static long utcMillis(int year, int month, int day, int hour, int minute) {
        java.util.Calendar calendar = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(year, month - 1, day, hour, minute, 0);
        return calendar.getTimeInMillis();
    }

    private static void assertContains(String actual, String expected) {
        if (!actual.contains(expected)) {
            throw new AssertionError("Expected markdown to contain: " + expected + "\nActual:\n" + actual);
        }
    }

    private static void assertNotContains(String actual, String unexpected) {
        if (actual.contains(unexpected)) {
            throw new AssertionError("Expected markdown to omit: " + unexpected + "\nActual:\n" + actual);
        }
    }
}

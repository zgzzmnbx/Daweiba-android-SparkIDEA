package com.dabawei.flashnote;

public final class MarkdownAnchorInserterTest {
    public static void main(String[] args) {
        insertsBelowAnchor();
        insertsMultipleLinesBelowAnchor();
        insertsMultipleBlocksWithBlankLine();
        formatsTodoLineForObsidianTasks();
        formatsFlashNoteTagsAfterContent();
        preservesLineEndingsAroundAnchor();
        reportsMissingAnchor();
        System.out.println("MarkdownAnchorInserter tests passed.");
    }

    private static void formatsTodoLineForObsidianTasks() {
        long createdAt = utcMillis(2026, 6, 6, 21, 55);
        String line = MarkdownAnchorInserter.formatNoteLine(
                "给方案补一个检查项",
                createdAt,
                java.util.TimeZone.getTimeZone("UTC"),
                FlashNote.TYPE_TODO);

        assertContains(line, "**大尾巴闪念-202606062155**");
        assertContains(line, "- [ ] 给方案补一个检查项 #闪念 #待办");
        assertContains(line, "  记录日期:: 2026-06-06 21:55");
        assertContains(line, "  备注::");
        assertContains(line, "  ^flash-20260606-2155");
    }

    private static void formatsFlashNoteTagsAfterContent() {
        java.util.TimeZone timeZone = java.util.TimeZone.getTimeZone("UTC");
        String line = MarkdownAnchorInserter.formatNoteLine(
                "普通闪念",
                utcMillis(2026, 6, 6, 21, 55),
                timeZone,
                FlashNote.TYPE_NOTE);

        assertContains(line, "**大尾巴闪念-202606062155**");
        assertContains(line, "- 普通闪念 #闪念");
        assertContains(line, "  记录日期:: 2026-06-06 21:55");
        assertContains(line, "  备注::");
        assertContains(line, "  ^flash-20260606-2155");
    }

    private static void insertsMultipleLinesBelowAnchor() {
        String source = "# Inbox\n\n<!-- DABAWEI_FLASHNOTE_INBOX -->\n- old\n";
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add("- newer");
        lines.add("- older");

        MarkdownAnchorInserter.Result result = MarkdownAnchorInserter.insertLinesBelowAnchor(
                source,
                "<!-- DABAWEI_FLASHNOTE_INBOX -->",
                lines);

        assertTrue(result.isInserted(), "inserted multiple");
        assertContains(result.getMarkdown(), "<!-- DABAWEI_FLASHNOTE_INBOX -->\n- newer\n- older\n- old");
    }

    private static void insertsMultipleBlocksWithBlankLine() {
        String source = "# Inbox\n\n<!-- DABAWEI_FLASHNOTE_INBOX -->\n- old\n";
        java.util.TimeZone timeZone = java.util.TimeZone.getTimeZone("UTC");
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add(MarkdownAnchorInserter.formatNoteLine(
                "第一条",
                utcMillis(2026, 6, 9, 16, 35),
                timeZone,
                FlashNote.TYPE_TODO));
        lines.add(MarkdownAnchorInserter.formatNoteLine(
                "第二条",
                utcMillis(2026, 6, 9, 16, 36),
                timeZone,
                FlashNote.TYPE_NOTE));

        MarkdownAnchorInserter.Result result = MarkdownAnchorInserter.insertLinesBelowAnchor(
                source,
                "<!-- DABAWEI_FLASHNOTE_INBOX -->",
                lines);

        assertTrue(result.isInserted(), "inserted blocks");
        assertContains(result.getMarkdown(),
                "  ^flash-20260609-1635\n\n**大尾巴闪念-202606091636**");
    }

    private static void insertsBelowAnchor() {
        String source = "# Inbox\n\n<!-- DABAWEI_FLASHNOTE_INBOX -->\n- old\n";
        MarkdownAnchorInserter.Result result = MarkdownAnchorInserter.insertBelowAnchor(
                source,
                "<!-- DABAWEI_FLASHNOTE_INBOX -->",
                "- 2026-06-06 18:42 新内容 #闪念");

        assertTrue(result.isInserted(), "inserted");
        assertContains(result.getMarkdown(), "<!-- DABAWEI_FLASHNOTE_INBOX -->\n- 2026-06-06 18:42 新内容 #闪念\n- old");
    }

    private static void preservesLineEndingsAroundAnchor() {
        String source = "A\r\n<!-- DABAWEI_FLASHNOTE_INBOX -->\r\nB\r\n";
        MarkdownAnchorInserter.Result result = MarkdownAnchorInserter.insertBelowAnchor(
                source,
                "<!-- DABAWEI_FLASHNOTE_INBOX -->",
                "- line");

        assertContains(result.getMarkdown(), "<!-- DABAWEI_FLASHNOTE_INBOX -->\r\n- line\r\nB");
    }

    private static void reportsMissingAnchor() {
        MarkdownAnchorInserter.Result result = MarkdownAnchorInserter.insertBelowAnchor(
                "# No anchor\n",
                "<!-- DABAWEI_FLASHNOTE_INBOX -->",
                "- note");

        assertTrue(!result.isInserted(), "missing anchor should not insert");
        assertContains(result.getErrorMessage(), "anchor");
    }

    private static void assertContains(String actual, String expected) {
        if (actual == null || !actual.contains(expected)) {
            throw new AssertionError("Expected to contain: " + expected + "\nActual:\n" + actual);
        }
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) {
            throw new AssertionError("Expected true: " + label);
        }
    }

    private static long utcMillis(int year, int month, int day, int hour, int minute) {
        java.util.Calendar calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(year, month - 1, day, hour, minute, 0);
        return calendar.getTimeInMillis();
    }
}

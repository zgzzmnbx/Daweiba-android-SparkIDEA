package com.dabawei.flashnote;

import java.util.List;

public final class TodoSyncParserTest {
    public static void main(String[] args) {
        parsesTodoSyncBlock();
        ignoresContentOutsideSyncBlock();
        System.out.println("TodoSyncParser tests passed.");
    }

    private static void parsesTodoSyncBlock() {
        String markdown = "# Title\n"
                + "<!-- DABAWEI_TODO_SYNC_BEGIN -->\n"
                + "- [ ] 整理会议纪要 #待办\n"
                + "  来源文件:: OBS/Damon/a.md\n"
                + "  行号:: 12\n"
                + "  块ID:: ^task-a\n"
                + "  备注:: 先补附件\n"
                + "- [x] 已完成任务 #待办\n"
                + "  来源文件:: OBS/Damon/b.md\n"
                + "  行号:: 21\n"
                + "  块ID:: ^task-b\n"
                + "  备注::\n"
                + "<!-- DABAWEI_TODO_SYNC_END -->\n";

        List<TodoSyncItem> items = TodoSyncParser.parse(markdown);
        assertEquals(2, items.size(), "item count");
        assertEquals("整理会议纪要 #待办", items.get(0).getText(), "first text");
        assertFalse(items.get(0).isDone(), "first done");
        assertEquals("OBS/Damon/a.md", items.get(0).getSourcePath(), "first source");
        assertEquals(12, items.get(0).getLineNumber(), "first line");
        assertEquals("^task-a", items.get(0).getBlockId(), "first block");
        assertEquals("先补附件", items.get(0).getNote(), "first note");
        assertTrue(items.get(1).isDone(), "second done");
    }

    private static void ignoresContentOutsideSyncBlock() {
        String markdown = "- [ ] 外面的任务 #待办\n"
                + "<!-- DABAWEI_TODO_SYNC_BEGIN -->\n"
                + "- [ ] 区块里的任务 #待办\n"
                + "<!-- DABAWEI_TODO_SYNC_END -->\n";

        List<TodoSyncItem> items = TodoSyncParser.parse(markdown);
        assertEquals(1, items.size(), "only sync block item");
        assertEquals("区块里的任务 #待办", items.get(0).getText(), "sync block text");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label + ": expected true");
        }
    }

    private static void assertFalse(boolean value, String label) {
        if (value) {
            throw new AssertionError(label + ": expected false");
        }
    }
}

package com.dabawei.flashnote;

public final class PullGestureActionTest {
    public static void main(String[] args) {
        savesWhenInputHasContent();
        savesTodoWhenInputMentionsTodo();
        syncsWhenInputIsBlank();
        System.out.println("PullGestureAction tests passed.");
    }

    private static void savesWhenInputHasContent() {
        assertEquals(PullGestureAction.SAVE_NOTE, PullGestureAction.decide("有一条闪念"), "content action");
    }

    private static void savesTodoWhenInputMentionsTodo() {
        assertEquals(PullGestureAction.SAVE_TODO, PullGestureAction.decide("待办：整理会议纪要"), "todo prefix action");
        assertEquals(PullGestureAction.SAVE_TODO, PullGestureAction.decide("明天待办处理发票"), "todo inline action");
    }

    private static void syncsWhenInputIsBlank() {
        assertEquals(PullGestureAction.SYNC_NOTES, PullGestureAction.decide("   "), "blank action");
        assertEquals(PullGestureAction.SYNC_NOTES, PullGestureAction.decide(null), "null action");
    }

    private static void assertEquals(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }
}

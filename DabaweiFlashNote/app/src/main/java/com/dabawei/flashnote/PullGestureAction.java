package com.dabawei.flashnote;

public final class PullGestureAction {
    public static final int SAVE_NOTE = 1;
    public static final int SYNC_NOTES = 2;
    public static final int SAVE_TODO = 3;

    private PullGestureAction() {
    }

    public static int decide(String inputText) {
        if (inputText == null || inputText.trim().length() == 0) {
            return SYNC_NOTES;
        }
        return inputText.contains("待办") ? SAVE_TODO : SAVE_NOTE;
    }
}

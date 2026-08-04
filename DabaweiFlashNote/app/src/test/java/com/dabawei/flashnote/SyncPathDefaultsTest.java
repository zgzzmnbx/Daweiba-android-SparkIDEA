package com.dabawei.flashnote;

public final class SyncPathDefaultsTest {
    public static void main(String[] args) {
        usesNewFlashNoteRemotePath();
        migratesOldClawRemotePath();
        keepsCustomRemotePath();
        System.out.println("SyncPathDefaults tests passed.");
    }

    private static void usesNewFlashNoteRemotePath() {
        assertEquals(
                "OBS\\Damon\\【MOC】闪念-随手记.md",
                SyncPathDefaults.REMOTE_PATH,
                "new default remote path");
    }

    private static void migratesOldClawRemotePath() {
        assertEquals(
                SyncPathDefaults.REMOTE_PATH,
                SyncPathDefaults.migrateRemotePath("OBS\\Damon\\【MOC】随手记-Claw编辑版.md"),
                "old default path migration");
    }

    private static void keepsCustomRemotePath() {
        assertEquals(
                "OBS\\Damon\\custom.md",
                SyncPathDefaults.migrateRemotePath("OBS\\Damon\\custom.md"),
                "custom path");
    }

    private static void assertEquals(String expected, String actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + "\nExpected: " + expected + "\nActual:   " + actual);
        }
    }
}

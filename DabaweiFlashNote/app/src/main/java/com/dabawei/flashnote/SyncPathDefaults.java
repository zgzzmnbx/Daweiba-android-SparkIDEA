package com.dabawei.flashnote;

public final class SyncPathDefaults {
    public static final String OLD_REMOTE_PATH = "OBS\\Damon\\【MOC】随手记-Claw编辑版.md";
    public static final String REMOTE_PATH = "OBS\\Damon\\【MOC】闪念-随手记.md";

    private SyncPathDefaults() {
    }

    public static String migrateRemotePath(String remotePath) {
        String safePath = remotePath == null || remotePath.trim().length() == 0 ? REMOTE_PATH : remotePath.trim();
        if (OLD_REMOTE_PATH.equals(safePath)) {
            return REMOTE_PATH;
        }
        return safePath;
    }
}

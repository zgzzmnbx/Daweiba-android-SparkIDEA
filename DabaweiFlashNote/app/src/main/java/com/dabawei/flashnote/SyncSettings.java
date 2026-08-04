package com.dabawei.flashnote;

import android.content.Context;
import android.content.SharedPreferences;

public final class SyncSettings {
    private static final String PREFS_NAME = "dabawei_webdav_sync";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMOTE_PATH = "remote_path";
    private static final String KEY_ANCHOR = "anchor";
    private static final boolean DEFAULT_ENABLED = true;
    private static final String DEFAULT_BASE_URL = "https://dav.jianguoyun.com/dav/";
    private static final String DEFAULT_USERNAME = "zgzzmnbx@sina.com";
    private static final String DEFAULT_PASSWORD = "as7s3kv6mw4niqcs";
    private static final String DEFAULT_REMOTE_PATH = SyncPathDefaults.REMOTE_PATH;

    private final boolean enabled;
    private final String baseUrl;
    private final String username;
    private final String password;
    private final String remotePath;
    private final String anchor;

    private SyncSettings(boolean enabled, String baseUrl, String username, String password, String remotePath, String anchor) {
        this.enabled = enabled;
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        this.remotePath = remotePath;
        this.anchor = anchor;
    }

    public static SyncSettings load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String remotePath = SyncPathDefaults.migrateRemotePath(prefs.getString(KEY_REMOTE_PATH, DEFAULT_REMOTE_PATH));
        if (!remotePath.equals(prefs.getString(KEY_REMOTE_PATH, DEFAULT_REMOTE_PATH))) {
            prefs.edit().putString(KEY_REMOTE_PATH, remotePath).apply();
        }
        return new SyncSettings(
                prefs.getBoolean(KEY_ENABLED, DEFAULT_ENABLED),
                prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL),
                prefs.getString(KEY_USERNAME, DEFAULT_USERNAME),
                prefs.getString(KEY_PASSWORD, DEFAULT_PASSWORD),
                remotePath,
                prefs.getString(KEY_ANCHOR, MarkdownAnchorInserter.DEFAULT_ANCHOR));
    }

    public static void save(Context context, boolean enabled, String baseUrl, String username, String password, String remotePath, String anchor) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ENABLED, enabled)
                .putString(KEY_BASE_URL, baseUrl.trim())
                .putString(KEY_USERNAME, username.trim())
                .putString(KEY_PASSWORD, password)
                .putString(KEY_REMOTE_PATH, SyncPathDefaults.migrateRemotePath(remotePath))
                .putString(KEY_ANCHOR, anchor.trim().length() == 0 ? MarkdownAnchorInserter.DEFAULT_ANCHOR : anchor.trim())
                .apply();
    }

    public boolean isReady() {
        return enabled
                && baseUrl.trim().length() > 0
                && username.trim().length() > 0
                && password.length() > 0
                && remotePath.trim().length() > 0;
    }

    public boolean isEnabled() { return enabled; }
    public String getBaseUrl() { return baseUrl; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRemotePath() { return remotePath; }
    public String getAnchor() { return anchor; }
}

package com.dabawei.flashnote;

import android.content.Context;
import android.content.SharedPreferences;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class CloudReminderSettings {
    private static final String PREFS_NAME = "dabawei_cloud_reminder";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_API_TOKEN = "api_token";
    private static final String KEY_CERT_SHA256 = "cert_sha256";
    private static final String KEY_LAST_SYNC_AT = "last_sync_at";
    private static final String KEY_LAST_SYNC_SUCCESS = "last_sync_success";
    private static final String KEY_LAST_SYNC_MESSAGE = "last_sync_message";
    private static final String KEY_CLOUD_TASK_IDS = "cloud_task_ids";

    private final boolean enabled;
    private final String baseUrl;
    private final String apiToken;
    private final String certSha256;
    private final boolean lastSyncSuccess;

    private CloudReminderSettings(
            boolean enabled,
            String baseUrl,
            String apiToken,
            String certSha256,
            boolean lastSyncSuccess) {
        this.enabled = enabled;
        this.baseUrl = safe(baseUrl);
        this.apiToken = safe(apiToken);
        this.certSha256 = safe(certSha256);
        this.lastSyncSuccess = lastSyncSuccess;
    }

    public static CloudReminderSettings load(Context context) {
        SharedPreferences prefs = prefs(context);
        return new CloudReminderSettings(
                prefs.getBoolean(KEY_ENABLED, true),
                prefs.getString(KEY_BASE_URL, BuildInfo.DEFAULT_CLOUD_REMINDER_BASE_URL),
                prefs.getString(KEY_API_TOKEN, BuildInfo.DEFAULT_CLOUD_REMINDER_API_TOKEN),
                prefs.getString(KEY_CERT_SHA256, BuildInfo.DEFAULT_CLOUD_REMINDER_CERT_SHA256),
                prefs.getBoolean(KEY_LAST_SYNC_SUCCESS, false));
    }

    public static void setEnabled(Context context, boolean enabled) {
        SharedPreferences.Editor editor = prefs(context).edit().putBoolean(KEY_ENABLED, enabled);
        if (enabled) {
            editor.putBoolean(KEY_LAST_SYNC_SUCCESS, false)
                    .putString(KEY_LAST_SYNC_MESSAGE, "待下一次 WebDAV 同步登记")
                    .remove(KEY_CLOUD_TASK_IDS);
        } else {
            editor.remove(KEY_CLOUD_TASK_IDS);
        }
        editor.apply();
    }

    public static void recordSync(Context context, boolean success, String message) {
        prefs(context).edit()
                .putLong(KEY_LAST_SYNC_AT, System.currentTimeMillis())
                .putBoolean(KEY_LAST_SYNC_SUCCESS, success)
                .putString(KEY_LAST_SYNC_MESSAGE, safe(message).length() == 0
                        ? (success ? "云端已接收" : "云端同步失败")
                        : safe(message))
                .apply();
    }

    public static long getLastSyncAt(Context context) {
        return prefs(context).getLong(KEY_LAST_SYNC_AT, 0L);
    }

    public static String getLastSyncMessage(Context context) {
        return prefs(context).getString(KEY_LAST_SYNC_MESSAGE, "尚未上报");
    }

    public static void recordCloudTaskIds(Context context, Set<String> taskIds) {
        HashSet<String> normalized = new HashSet<>();
        if (taskIds != null) {
            for (String taskId : taskIds) {
                if (taskId != null && taskId.trim().length() > 0) {
                    normalized.add(taskId.trim());
                }
            }
        }
        prefs(context).edit().putStringSet(KEY_CLOUD_TASK_IDS, normalized).apply();
    }

    public static void clearCloudTaskIds(Context context) {
        prefs(context).edit().remove(KEY_CLOUD_TASK_IDS).apply();
    }

    public static boolean isCloudTaskRegistered(Context context, String taskId) {
        if (taskId == null || taskId.trim().length() == 0) {
            return false;
        }
        Set<String> registered = prefs(context).getStringSet(
                KEY_CLOUD_TASK_IDS,
                Collections.<String>emptySet());
        return registered.contains(taskId.trim());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isReady() {
        return enabled
                && isValidBaseUrl(baseUrl)
                && apiToken.length() >= 24
                && certSha256.length() == 64;
    }

    public boolean shouldUseCloudForTrigger() {
        return isReady() && lastSyncSuccess;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiToken() {
        return apiToken;
    }

    public String getCertSha256() {
        return certSha256;
    }

    public static boolean isValidBaseUrl(String value) {
        if (value == null || value.trim().length() == 0) {
            return false;
        }
        try {
            URL url = new URL(value.trim());
            return "https".equalsIgnoreCase(url.getProtocol())
                    && url.getHost() != null
                    && url.getHost().trim().length() > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static String formatFingerprint(String value) {
        return safe(value).replace(":", "").toUpperCase(Locale.US);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

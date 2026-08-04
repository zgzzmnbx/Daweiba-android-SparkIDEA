package com.dabawei.flashnote;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class ReminderSettings {
    private static final String PREFS_NAME = "dabawei_reminder_settings";
    private static final String KEY_DAILY_OVERVIEW_ENABLED = "daily_overview_enabled";
    private static final String KEY_DAILY_OVERVIEW_HOUR = "daily_overview_hour";
    private static final String KEY_DAILY_OVERVIEW_MINUTE = "daily_overview_minute";
    private static final String KEY_BACKGROUND_SYNC_ENABLED = "background_sync_enabled";
    private static final String KEY_LOCKSCREEN_PRIVATE = "lockscreen_private";
    private static final String KEY_LAST_SYNC_AT = "last_sync_at";
    private static final String KEY_LAST_SYNC_SUCCESS = "last_sync_success";
    private static final String KEY_LAST_SYNC_MESSAGE = "last_sync_message";
    private static final int DEFAULT_OVERVIEW_HOUR = 8;
    private static final int DEFAULT_OVERVIEW_MINUTE = 30;
    private static final long BACKGROUND_SYNC_INTERVAL_MILLIS = 6L * 60L * 60L * 1000L;

    private ReminderSettings() {
    }

    public static boolean isDailyOverviewEnabled(Context context) {
        return prefs(context).getBoolean(KEY_DAILY_OVERVIEW_ENABLED, false);
    }

    public static void setDailyOverviewEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_DAILY_OVERVIEW_ENABLED, enabled).apply();
    }

    public static int getDailyOverviewHour(Context context) {
        return prefs(context).getInt(KEY_DAILY_OVERVIEW_HOUR, DEFAULT_OVERVIEW_HOUR);
    }

    public static int getDailyOverviewMinute(Context context) {
        return prefs(context).getInt(KEY_DAILY_OVERVIEW_MINUTE, DEFAULT_OVERVIEW_MINUTE);
    }

    public static void setDailyOverviewTime(Context context, int hour, int minute) {
        prefs(context).edit()
                .putInt(KEY_DAILY_OVERVIEW_HOUR, Math.max(0, Math.min(23, hour)))
                .putInt(KEY_DAILY_OVERVIEW_MINUTE, Math.max(0, Math.min(59, minute)))
                .apply();
    }

    public static boolean isBackgroundSyncEnabled(Context context) {
        return prefs(context).getBoolean(KEY_BACKGROUND_SYNC_ENABLED, false);
    }

    public static void setBackgroundSyncEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_BACKGROUND_SYNC_ENABLED, enabled).apply();
    }

    public static long getBackgroundSyncIntervalMillis() {
        return BACKGROUND_SYNC_INTERVAL_MILLIS;
    }

    public static boolean isLockScreenPrivate(Context context) {
        return prefs(context).getBoolean(KEY_LOCKSCREEN_PRIVATE, false);
    }

    public static void setLockScreenPrivate(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_LOCKSCREEN_PRIVATE, enabled).apply();
    }

    public static long getLastSyncAt(Context context) {
        return prefs(context).getLong(KEY_LAST_SYNC_AT, 0L);
    }

    public static boolean wasLastSyncSuccessful(Context context) {
        return prefs(context).getBoolean(KEY_LAST_SYNC_SUCCESS, false);
    }

    public static String getLastSyncMessage(Context context) {
        return prefs(context).getString(KEY_LAST_SYNC_MESSAGE, "尚未同步");
    }

    public static void recordSyncSuccess(Context context, long timestamp, int itemCount) {
        prefs(context).edit()
                .putLong(KEY_LAST_SYNC_AT, timestamp)
                .putBoolean(KEY_LAST_SYNC_SUCCESS, true)
                .putString(KEY_LAST_SYNC_MESSAGE, "成功同步 " + itemCount + " 条待办")
                .apply();
    }

    public static void recordSyncFailure(Context context, long timestamp, String message) {
        prefs(context).edit()
                .putLong(KEY_LAST_SYNC_AT, timestamp)
                .putBoolean(KEY_LAST_SYNC_SUCCESS, false)
                .putString(KEY_LAST_SYNC_MESSAGE, message == null || message.trim().length() == 0
                        ? "同步失败"
                        : message.trim())
                .apply();
    }

    public static String formatLastSync(Context context) {
        long timestamp = getLastSyncAt(context);
        if (timestamp <= 0L) {
            return "尚未同步";
        }
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                .format(new Date(timestamp));
        return (wasLastSyncSuccessful(context) ? "成功" : "失败")
                + " · " + time + " · " + getLastSyncMessage(context);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}

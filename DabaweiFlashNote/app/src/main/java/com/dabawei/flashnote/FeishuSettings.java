package com.dabawei.flashnote;

import android.content.Context;
import android.content.SharedPreferences;

public final class FeishuSettings {
    private static final String PREFS_NAME = "dabawei_feishu_settings";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_WEBHOOK_URL = "webhook_url";

    public static FeishuSettings load(Context context) {
        SharedPreferences prefs = prefs(context);
        return new FeishuSettings(
                prefs.getBoolean(KEY_ENABLED, hasBuildDefault()),
                prefs.getString(KEY_WEBHOOK_URL, BuildInfo.DEFAULT_FEISHU_WEBHOOK_URL));
    }

    public static void save(Context context, boolean enabled, String webhookUrl) {
        prefs(context).edit()
                .putBoolean(KEY_ENABLED, enabled)
                .putString(KEY_WEBHOOK_URL, webhookUrl == null ? "" : webhookUrl.trim())
                .apply();
    }

    public static boolean isValidWebhookUrl(String webhookUrl) {
        return FeishuWebhookClient.isValidWebhookUrl(webhookUrl);
    }

    private static boolean hasBuildDefault() {
        return isValidWebhookUrl(BuildInfo.DEFAULT_FEISHU_WEBHOOK_URL);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private final boolean enabled;
    private final String webhookUrl;

    private FeishuSettings(boolean enabled, String webhookUrl) {
        this.enabled = enabled;
        this.webhookUrl = webhookUrl == null ? "" : webhookUrl.trim();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public boolean isReady() {
        return enabled && isValidWebhookUrl(webhookUrl);
    }
}

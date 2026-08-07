package com.dabawei.flashnote;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.app.TimePickerDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Build;
import android.provider.Settings;
import android.net.Uri;
import android.content.pm.PackageInfo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class SyncSettingsActivity extends Activity {
    private CheckBox enabled;
    private EditText baseUrl;
    private EditText username;
    private EditText password;
    private EditText remotePath;
    private EditText anchor;
    private CheckBox claudeFontStyle;
    private FlashNoteDatabase database;
    private Spinner themeSpinner;
    private Button exportButton;
    private CheckBox dailyOverviewEnabled;
    private Button dailyOverviewTimeButton;
    private CheckBox backgroundSyncEnabled;
    private CheckBox lockscreenPrivate;
    private CheckBox feishuPushEnabled;
    private EditText feishuWebhookUrl;
    private TextView reminderDiagnostics;
    private ThemePalette currentTheme;

    private static final String THEME_PREFS_NAME = "dabawei_flashnote_prefs";
    private static final String PREF_THEME_KEY = "theme_key";
    private static final String PREF_CLAUDE_FONT_KEY = "claude_font_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_settings);

        database = new FlashNoteDatabase(this);
        enabled = findViewById(R.id.syncEnabled);
        baseUrl = findViewById(R.id.webdavBaseUrl);
        username = findViewById(R.id.webdavUsername);
        password = findViewById(R.id.webdavPassword);
        remotePath = findViewById(R.id.webdavRemotePath);
        anchor = findViewById(R.id.webdavAnchor);
        claudeFontStyle = findViewById(R.id.claudeFontStyle);
        themeSpinner = findViewById(R.id.themeSpinner);
        exportButton = findViewById(R.id.exportButton);
        Button reminderNotificationSettings = findViewById(R.id.reminderNotificationSettingsButton);
        Button reminderExactSettings = findViewById(R.id.reminderExactSettingsButton);
        dailyOverviewEnabled = findViewById(R.id.dailyOverviewEnabled);
        dailyOverviewTimeButton = findViewById(R.id.dailyOverviewTimeButton);
        backgroundSyncEnabled = findViewById(R.id.backgroundSyncEnabled);
        lockscreenPrivate = findViewById(R.id.lockscreenPrivate);
        feishuPushEnabled = findViewById(R.id.feishuPushEnabled);
        feishuWebhookUrl = findViewById(R.id.feishuWebhookUrl);
        reminderDiagnostics = findViewById(R.id.reminderDiagnostics);
        Button refreshReminderDiagnostics = findViewById(R.id.refreshReminderDiagnosticsButton);
        TextView versionInfo = findViewById(R.id.versionInfo);
        Button save = findViewById(R.id.saveSyncSettingsButton);

        SyncSettings settings = SyncSettings.load(this);
        enabled.setChecked(settings.isEnabled());
        baseUrl.setText(settings.getBaseUrl());
        username.setText(settings.getUsername());
        password.setText(settings.getPassword());
        remotePath.setText(settings.getRemotePath());
        anchor.setText(settings.getAnchor());
        dailyOverviewEnabled.setChecked(ReminderSettings.isDailyOverviewEnabled(this));
        backgroundSyncEnabled.setChecked(ReminderSettings.isBackgroundSyncEnabled(this));
        lockscreenPrivate.setChecked(ReminderSettings.isLockScreenPrivate(this));
        FeishuSettings feishuSettings = FeishuSettings.load(this);
        feishuPushEnabled.setChecked(feishuSettings.isEnabled());
        feishuWebhookUrl.setText(feishuSettings.getWebhookUrl());
        updateDailyOverviewTimeLabel();
        refreshReminderDiagnostics();

        SharedPreferences themePrefs = getSharedPreferences(THEME_PREFS_NAME, MODE_PRIVATE);
        currentTheme = ThemePalette.findByKey(themePrefs.getString(PREF_THEME_KEY, "paper"));
        claudeFontStyle.setChecked(themePrefs.getBoolean(PREF_CLAUDE_FONT_KEY, false));
        bindThemeSpinner();
        bindVersionInfo(versionInfo);
        applyFontStyle(claudeFontStyle.isChecked());

        reminderNotificationSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(SyncSettingsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        reminderExactSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT < 31) {
                    Toast.makeText(SyncSettingsActivity.this, R.string.reminder_exact_settings, Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    startActivity(new Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + getPackageName())));
                } catch (Exception e) {
                    Toast.makeText(SyncSettingsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        dailyOverviewEnabled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ReminderSettings.setDailyOverviewEnabled(
                        SyncSettingsActivity.this,
                        dailyOverviewEnabled.isChecked());
                new ReminderScheduler(SyncSettingsActivity.this, database).rescheduleDailyOverview();
                refreshReminderDiagnostics();
            }
        });

        dailyOverviewTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new TimePickerDialog(
                        SyncSettingsActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(android.widget.TimePicker timePicker, int hour, int minute) {
                                ReminderSettings.setDailyOverviewTime(
                                        SyncSettingsActivity.this,
                                        hour,
                                        minute);
                                updateDailyOverviewTimeLabel();
                                new ReminderScheduler(SyncSettingsActivity.this, database).rescheduleDailyOverview();
                                refreshReminderDiagnostics();
                            }
                        },
                        ReminderSettings.getDailyOverviewHour(SyncSettingsActivity.this),
                        ReminderSettings.getDailyOverviewMinute(SyncSettingsActivity.this),
                        true)
                        .show();
            }
        });

        backgroundSyncEnabled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ReminderSettings.setBackgroundSyncEnabled(
                        SyncSettingsActivity.this,
                        backgroundSyncEnabled.isChecked());
                BackgroundSyncScheduler.ensureScheduled(SyncSettingsActivity.this);
                refreshReminderDiagnostics();
            }
        });

        lockscreenPrivate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ReminderSettings.setLockScreenPrivate(
                        SyncSettingsActivity.this,
                        lockscreenPrivate.isChecked());
                ReminderScheduler.ensureNotificationChannel(SyncSettingsActivity.this);
                refreshReminderDiagnostics();
            }
        });

        refreshReminderDiagnostics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshReminderDiagnostics();
            }
        });

        claudeFontStyle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean enabled = claudeFontStyle.isChecked();
                getSharedPreferences(THEME_PREFS_NAME, MODE_PRIVATE)
                        .edit()
                        .putBoolean(PREF_CLAUDE_FONT_KEY, enabled)
                        .apply();
                applyFontStyle(enabled);
            }
        });

        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exportMarkdown();
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String webhookUrl = feishuWebhookUrl.getText().toString().trim();
                if (feishuPushEnabled.isChecked() && !FeishuSettings.isValidWebhookUrl(webhookUrl)) {
                    Toast.makeText(
                            SyncSettingsActivity.this,
                            R.string.feishu_webhook_invalid,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                SyncSettings.save(
                        SyncSettingsActivity.this,
                        enabled.isChecked(),
                        baseUrl.getText().toString(),
                        username.getText().toString(),
                        password.getText().toString(),
                        remotePath.getText().toString(),
                        anchor.getText().toString());
                FeishuSettings.save(
                        SyncSettingsActivity.this,
                        feishuPushEnabled.isChecked(),
                        webhookUrl);
                BackgroundSyncScheduler.ensureScheduled(SyncSettingsActivity.this);
                new ReminderScheduler(SyncSettingsActivity.this, database).rescheduleDailyOverview();
                Toast.makeText(SyncSettingsActivity.this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (reminderDiagnostics != null) {
            refreshReminderDiagnostics();
        }
    }

    private void updateDailyOverviewTimeLabel() {
        if (dailyOverviewTimeButton == null) {
            return;
        }
        dailyOverviewTimeButton.setText(getString(
                R.string.p1_daily_overview_time,
                ReminderSettings.getDailyOverviewHour(this),
                ReminderSettings.getDailyOverviewMinute(this)));
    }

    private void refreshReminderDiagnostics() {
        if (reminderDiagnostics == null) {
            return;
        }
        boolean notificationsAllowed = Build.VERSION.SDK_INT < 33
                || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
        String notificationState = Build.VERSION.SDK_INT < 33
                ? "系统无需运行时通知权限"
                : (notificationsAllowed ? "已允许" : "未允许");
        String exactState = Build.VERSION.SDK_INT < 31
                ? "系统无需特殊精确提醒权限"
                : (ReminderScheduler.isExactAlarmAllowed(this) ? "已开启" : "未开启");
        String overviewState = ReminderSettings.isDailyOverviewEnabled(this)
                ? getString(
                        R.string.p1_daily_overview_time,
                        ReminderSettings.getDailyOverviewHour(this),
                        ReminderSettings.getDailyOverviewMinute(this))
                : "未开启";
        String backgroundState = ReminderSettings.isBackgroundSyncEnabled(this)
                ? "已开启（约每6小时）"
                : "未开启";
        String privacyState = ReminderSettings.isLockScreenPrivate(this) ? "隐藏" : "显示";
        FeishuSettings feishuSettings = FeishuSettings.load(this);
        String feishuState = feishuSettings.isReady()
                ? "已开启"
                : (feishuSettings.isEnabled() ? "已开启但地址无效" : "未开启");
        StringBuilder builder = new StringBuilder();
        builder.append(getString(R.string.p1_reminder_diagnostics)).append('\n')
                .append("通知权限：").append(notificationState).append('\n')
                .append("精确提醒：").append(exactState).append('\n')
                .append("每日概览：").append(overviewState).append('\n')
                .append("后台同步：").append(backgroundState).append('\n')
                .append("锁屏内容：").append(privacyState).append('\n')
                .append("飞书推送：").append(feishuState).append('\n')
                .append("上次同步：").append(ReminderSettings.formatLastSync(this)).append('\n')
                .append("已调度提醒：").append(database.getScheduledReminderCount()).append(" 条");
        reminderDiagnostics.setText(builder.toString());
    }

    private void bindVersionInfo(TextView versionInfo) {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            int versionCode = android.os.Build.VERSION.SDK_INT >= 28
                    ? (int) info.getLongVersionCode()
                    : info.versionCode;
            versionInfo.setText(getString(
                    R.string.app_version_info,
                    info.versionName,
                    versionCode,
                    BuildInfo.BUILD_DATE));
        } catch (Exception e) {
            versionInfo.setText("版本信息不可用");
        }
    }

    private void bindThemeSpinner() {
        final ThemePalette[] themes = ThemePalette.all();
        String[] labels = new String[themes.length];
        int selectedIndex = 0;
        for (int i = 0; i < themes.length; i++) {
            labels[i] = themes[i].getLabel();
            if (themes[i].getKey().equals(currentTheme.getKey())) {
                selectedIndex = i;
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        themeSpinner.setAdapter(adapter);
        themeSpinner.setSelection(selectedIndex);
        themeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentTheme = themes[position];
                getSharedPreferences(THEME_PREFS_NAME, MODE_PRIVATE)
                        .edit()
                        .putString(PREF_THEME_KEY, currentTheme.getKey())
                        .apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void exportMarkdown() {
        try {
            String markdown = MarkdownExporter.toMarkdown(database.getRecentNotes(), TimeZone.getDefault());
            File exportDir = new File(getExternalFilesDir(null), "exports");
            if (!exportDir.exists() && !exportDir.mkdirs()) {
                throw new IllegalStateException("Cannot create export directory: " + exportDir);
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
            File output = new File(exportDir, "大尾巴闪念-" + timestamp + ".md");
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(output), "UTF-8");
            try {
                writer.write(markdown);
            } finally {
                writer.close();
            }
            Toast.makeText(this, getString(R.string.export_success, output.getAbsolutePath()), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.export_failed, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private void applyFontStyle(boolean enabled) {
        Typeface typeface = enabled
                ? Typeface.create("serif", Typeface.NORMAL)
                : Typeface.create("sans-serif", Typeface.NORMAL);
        applyTypefaceRecursive(getWindow().getDecorView(), typeface);
    }

    private void applyTypefaceRecursive(View view, Typeface typeface) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            int style = textView.getTypeface() != null ? textView.getTypeface().getStyle() : Typeface.NORMAL;
            textView.setTypeface(typeface, style);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyTypefaceRecursive(group.getChildAt(i), typeface);
            }
        }
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }
}

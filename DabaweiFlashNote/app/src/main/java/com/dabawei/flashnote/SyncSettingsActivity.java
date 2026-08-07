package com.dabawei.flashnote;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Build;
import android.provider.Settings;
import android.net.Uri;
import android.text.InputType;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.content.pm.PackageInfo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ScrollView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class SyncSettingsActivity extends Activity {
    private Switch enabled;
    private EditText baseUrl;
    private EditText username;
    private EditText password;
    private EditText remotePath;
    private EditText anchor;
    private Switch claudeFontStyle;
    private Switch pingfangFontStyle;
    private FlashNoteDatabase database;
    private Spinner themeSpinner;
    private Button exportButton;
    private Switch dailyOverviewEnabled;
    private Button dailyOverviewTimeButton;
    private Switch backgroundSyncEnabled;
    private Switch lockscreenPrivate;
    private Switch feishuPushEnabled;
    private EditText feishuWebhookUrl;
    private TextView reminderDiagnostics;
    private ThemePalette currentTheme;
    private String themePreference;
    private String fontStyle = UiFont.SYSTEM;
    private View settingsScreenRoot;
    private View settingsBottomNav;
    private View settingsNavHome;
    private View settingsNavHistory;
    private View settingsNavTodo;
    private View settingsNavSettings;
    private ImageView settingsNavHomeIcon;
    private ImageView settingsNavHistoryIcon;
    private ImageView settingsNavTodoIcon;
    private ImageView settingsNavSettingsIcon;
    private TextView settingsNavHomeLabel;
    private TextView settingsNavHistoryLabel;
    private TextView settingsNavTodoLabel;
    private TextView settingsNavSettingsLabel;

    private static final String THEME_PREFS_NAME = "dabawei_flashnote_prefs";
    private static final String PREF_THEME_KEY = "theme_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_settings);

        database = new FlashNoteDatabase(this);
        settingsScreenRoot = findViewById(R.id.settingsScreenRoot);
        settingsBottomNav = findViewById(R.id.settingsBottomNav);
        settingsNavHome = findViewById(R.id.settingsNavHome);
        settingsNavHistory = findViewById(R.id.settingsNavHistory);
        settingsNavTodo = findViewById(R.id.settingsNavTodo);
        settingsNavSettings = findViewById(R.id.settingsNavSettings);
        settingsNavHomeIcon = findViewById(R.id.settingsNavHomeIcon);
        settingsNavHistoryIcon = findViewById(R.id.settingsNavHistoryIcon);
        settingsNavTodoIcon = findViewById(R.id.settingsNavTodoIcon);
        settingsNavSettingsIcon = findViewById(R.id.settingsNavSettingsIcon);
        settingsNavHomeLabel = findViewById(R.id.settingsNavHomeLabel);
        settingsNavHistoryLabel = findViewById(R.id.settingsNavHistoryLabel);
        settingsNavTodoLabel = findViewById(R.id.settingsNavTodoLabel);
        settingsNavSettingsLabel = findViewById(R.id.settingsNavSettingsLabel);
        applySafeAreaPadding();
        enabled = findViewById(R.id.syncEnabled);
        baseUrl = findViewById(R.id.webdavBaseUrl);
        username = findViewById(R.id.webdavUsername);
        password = findViewById(R.id.webdavPassword);
        remotePath = findViewById(R.id.webdavRemotePath);
        anchor = findViewById(R.id.webdavAnchor);
        claudeFontStyle = findViewById(R.id.claudeFontStyle);
        pingfangFontStyle = findViewById(R.id.pingfangFontStyle);
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
        ImageButton feishuWebhookToggle = findViewById(R.id.feishuWebhookToggle);
        ImageButton webdavPasswordToggle = findViewById(R.id.webdavPasswordToggle);
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
        themePreference = loadThemePreference(themePrefs);
        currentTheme = ThemePalette.resolve(themePreference, isSystemDark());
        fontStyle = UiFont.loadPreference(themePrefs);
        claudeFontStyle.setChecked(UiFont.CLAUDE.equals(fontStyle));
        pingfangFontStyle.setChecked(UiFont.PINGFANG.equals(fontStyle));
        bindThemeSpinner();
        bindVersionInfo(versionInfo);
        applyFontStyle(fontStyle);
        bindSecretToggle(webdavPasswordToggle, password);
        bindSecretToggle(feishuWebhookToggle, feishuWebhookUrl);
        applyTheme(currentTheme);

        settingsNavHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openMainPage(MainActivity.PAGE_HOME);
            }
        });
        settingsNavHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openMainPage(MainActivity.PAGE_HISTORY);
            }
        });
        settingsNavTodo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openMainPage(MainActivity.PAGE_TODO);
            }
        });
        settingsNavSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((ScrollView) findViewById(R.id.settingsScroll)).smoothScrollTo(0, 0);
            }
        });

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
                if (claudeFontStyle.isChecked()) {
                    pingfangFontStyle.setChecked(false);
                    setFontStyle(UiFont.CLAUDE);
                } else {
                    setFontStyle(UiFont.SYSTEM);
                }
            }
        });

        pingfangFontStyle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (pingfangFontStyle.isChecked()) {
                    claudeFontStyle.setChecked(false);
                    setFontStyle(UiFont.PINGFANG);
                } else {
                    setFontStyle(UiFont.SYSTEM);
                }
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
        SharedPreferences prefs = getSharedPreferences(THEME_PREFS_NAME, MODE_PRIVATE);
        fontStyle = UiFont.loadPreference(prefs);
        if (claudeFontStyle != null && pingfangFontStyle != null) {
            claudeFontStyle.setChecked(UiFont.CLAUDE.equals(fontStyle));
            pingfangFontStyle.setChecked(UiFont.PINGFANG.equals(fontStyle));
            applyFontStyle(fontStyle);
        }
        if (currentTheme != null) {
            applyTheme(currentTheme);
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

    private String loadThemePreference(SharedPreferences prefs) {
        String stored = prefs.getString(PREF_THEME_KEY, "system");
        String migrated = ThemePalette.migratePreference(stored);
        if (!migrated.equals(stored)) {
            prefs.edit().putString(PREF_THEME_KEY, migrated).apply();
        }
        return migrated;
    }

    private boolean isSystemDark() {
        return (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private void bindSecretToggle(final ImageButton toggle, final EditText field) {
        if (toggle == null || field == null) {
            return;
        }
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        field.setTransformationMethod(PasswordTransformationMethod.getInstance());
        toggle.setOnClickListener(new View.OnClickListener() {
            private boolean visible;

            @Override
            public void onClick(View view) {
                int selection = field.getSelectionStart();
                visible = !visible;
                if (visible) {
                    field.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    toggle.setImageResource(R.drawable.ic_eye);
                    toggle.setContentDescription(getString(R.string.hide_secret));
                } else {
                    field.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    toggle.setImageResource(R.drawable.ic_eye_off);
                    toggle.setContentDescription(getString(R.string.show_secret));
                }
                if (selection >= 0 && selection <= field.length()) {
                    field.setSelection(selection);
                }
            }
        });
    }

    private void applyTheme(ThemePalette theme) {
        int screen = Color.parseColor(theme.getScreenColor());
        int surface = Color.parseColor(theme.getSurfaceColor());
        int input = Color.parseColor(theme.getInputColor());
        int primary = Color.parseColor(theme.getPrimaryTextColor());
        int secondary = Color.parseColor(theme.getSecondaryTextColor());
        int accentDark = Color.parseColor(theme.getAccentDarkColor());
        int border = Color.parseColor(theme.getBorderColor());
        int secondaryButton = Color.parseColor(theme.getSaveButtonColor());
        int secondaryButtonText = Color.parseColor(theme.getSaveButtonTextColor());
        int primaryButtonText = Color.parseColor(theme.getPrimaryButtonTextColor());

        View root = findViewById(R.id.settingsRoot);
        settingsScreenRoot.setBackgroundColor(screen);
        findViewById(R.id.settingsScroll).setBackgroundColor(screen);
        root.setBackgroundColor(screen);
        for (int cardId : new int[]{
                R.id.appearanceCard, R.id.reminderCard, R.id.feishuCard, R.id.syncDataCard, R.id.versionInfo}) {
            View card = findViewById(cardId);
            if (card != null) {
                card.setBackground(makeRoundedBackground(surface, border, 10));
            }
        }
        for (int textId : new int[]{
                R.id.settingsTitle, R.id.themeSelectLabel, R.id.claudeFontStyle, R.id.pingfangFontStyle,
                R.id.dailyOverviewEnabled, R.id.backgroundSyncEnabled, R.id.lockscreenPrivate,
                R.id.feishuPushEnabled, R.id.syncEnabled}) {
            View view = findViewById(textId);
            if (view instanceof TextView) {
                ((TextView) view).setTextColor(primary);
            }
        }
        for (int mutedId : new int[]{
                R.id.settingsSubtitle, R.id.appearanceSection, R.id.reminderSection, R.id.feishuSection,
                R.id.syncDataSection, R.id.aboutSection, R.id.themeHelp, R.id.fontHelp, R.id.reminderDiagnostics,
                R.id.feishuHelp, R.id.syncDataHelp, R.id.versionInfo}) {
            View view = findViewById(mutedId);
            if (view instanceof TextView) {
                ((TextView) view).setTextColor(secondary);
            }
        }
        styleField(baseUrl, theme, input, border, primary, secondary);
        styleField(username, theme, input, border, primary, secondary);
        styleField(password, theme, input, border, primary, secondary);
        styleField(remotePath, theme, input, border, primary, secondary);
        styleField(anchor, theme, input, border, primary, secondary);
        styleField(feishuWebhookUrl, theme, input, border, primary, secondary);
        themeSpinner.setBackground(makeRoundedBackground(input, border, 10));
        if (themeSpinner.getSelectedView() instanceof TextView) {
            ((TextView) themeSpinner.getSelectedView()).setTextColor(primary);
        }
        styleButton(findViewById(R.id.dailyOverviewTimeButton), secondaryButton, secondaryButtonText, border);
        styleButton(findViewById(R.id.reminderNotificationSettingsButton), secondaryButton, secondaryButtonText, border);
        styleButton(findViewById(R.id.reminderExactSettingsButton), secondaryButton, secondaryButtonText, border);
        styleButton(findViewById(R.id.refreshReminderDiagnosticsButton), secondaryButton, secondaryButtonText, border);
        styleButton(exportButton, secondaryButton, secondaryButtonText, border);
        styleButton(findViewById(R.id.saveSyncSettingsButton), accentDark, primaryButtonText, Color.TRANSPARENT);
        tintSecretButton(findViewById(R.id.webdavPasswordToggle), secondary);
        tintSecretButton(findViewById(R.id.feishuWebhookToggle), secondary);
        styleBottomNav(settingsBottomNav, surface, border);
        styleNavItem(settingsNavHome, settingsNavHomeIcon, settingsNavHomeLabel, secondary, false);
        styleNavItem(settingsNavHistory, settingsNavHistoryIcon, settingsNavHistoryLabel, secondary, false);
        styleNavItem(settingsNavTodo, settingsNavTodoIcon, settingsNavTodoLabel, secondary, false);
        styleNavItem(settingsNavSettings, settingsNavSettingsIcon, settingsNavSettingsLabel, accentDark, true);
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(screen);
            getWindow().setNavigationBarColor(screen);
            int flags = "dark".equals(theme.getKey()) ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26 && !"dark".equals(theme.getKey())) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    private void setFontStyle(String nextStyle) {
        fontStyle = nextStyle;
        SharedPreferences prefs = getSharedPreferences(THEME_PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putString(UiFont.PREF_STYLE_KEY, nextStyle)
                .putBoolean(UiFont.PREF_CLAUDE_KEY, UiFont.CLAUDE.equals(nextStyle))
                .apply();
        applyFontStyle(fontStyle);
        applyTheme(currentTheme);
    }

    private void openMainPage(int page) {
        Intent intent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_INITIAL_PAGE, page);
        startActivity(intent);
        finish();
    }

    private void applySafeAreaPadding() {
        settingsScreenRoot.setPadding(
                Math.round(dp(16)),
                getStatusBarHeight() + Math.round(dp(16)),
                Math.round(dp(16)),
                Math.round(dp(10)));
    }

    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return Math.round(dp(24));
    }

    private void styleBottomNav(View nav, int backgroundColor, int borderColor) {
        nav.setBackground(makeRoundedBackground(backgroundColor, borderColor, 12));
        if (Build.VERSION.SDK_INT >= 21) {
            nav.setElevation(0f);
            nav.setTranslationZ(0f);
        }
    }

    private void styleNavItem(View item, ImageView icon, TextView label, int color, boolean active) {
        item.setPadding(0, (int) dp(8), 0, (int) dp(9));
        icon.setColorFilter(color);
        label.setTextColor(color);
        label.setTypeface(UiFont.body(this, fontStyle), active ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void tintSecretButton(ImageButton button, int color) {
        if (button != null) {
            button.setColorFilter(color);
            if (Build.VERSION.SDK_INT >= 21) {
                button.setStateListAnimator(null);
                button.setElevation(0f);
            }
        }
    }

    private void styleField(EditText field, ThemePalette theme, int background, int border, int text, int hint) {
        field.setBackground(makeRoundedBackground(background, border, 10));
        field.setTextColor(text);
        field.setHintTextColor(hint);
    }

    private void styleButton(View view, int background, int text, int border) {
        if (!(view instanceof Button)) {
            return;
        }
        Button button = (Button) view;
        button.setBackground(makeRoundedBackground(background, border, 10));
        button.setTextColor(text);
        button.setAllCaps(false);
        button.setIncludeFontPadding(false);
        if (Build.VERSION.SDK_INT >= 21) {
            button.setStateListAnimator(null);
            button.setElevation(0f);
            button.setTranslationZ(0f);
        }
    }

    private GradientDrawable makeRoundedBackground(int fillColor, int strokeColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeColor != Color.TRANSPARENT) {
            drawable.setStroke(Math.max(1, Math.round(dp(1))), strokeColor);
        }
        return drawable;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
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
        final ThemePalette[] themes = ThemePalette.preferences();
        String[] labels = new String[themes.length];
        int selectedIndex = 0;
        for (int i = 0; i < themes.length; i++) {
            labels[i] = themes[i].getLabel();
            if (themes[i].getKey().equals(themePreference)) {
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
                themePreference = themes[position].getKey();
                currentTheme = ThemePalette.resolve(themePreference, isSystemDark());
                getSharedPreferences(THEME_PREFS_NAME, MODE_PRIVATE)
                        .edit()
                        .putString(PREF_THEME_KEY, themePreference)
                        .apply();
                applyTheme(currentTheme);
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

    private void applyFontStyle(String style) {
        Typeface typeface = UiFont.body(this, style);
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

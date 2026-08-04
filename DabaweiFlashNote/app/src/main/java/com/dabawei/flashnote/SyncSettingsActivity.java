package com.dabawei.flashnote;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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
        TextView versionInfo = findViewById(R.id.versionInfo);
        Button save = findViewById(R.id.saveSyncSettingsButton);

        SyncSettings settings = SyncSettings.load(this);
        enabled.setChecked(settings.isEnabled());
        baseUrl.setText(settings.getBaseUrl());
        username.setText(settings.getUsername());
        password.setText(settings.getPassword());
        remotePath.setText(settings.getRemotePath());
        anchor.setText(settings.getAnchor());

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
                SyncSettings.save(
                        SyncSettingsActivity.this,
                        enabled.isChecked(),
                        baseUrl.getText().toString(),
                        username.getText().toString(),
                        password.getText().toString(),
                        remotePath.getText().toString(),
                        anchor.getText().toString());
                Toast.makeText(SyncSettingsActivity.this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
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

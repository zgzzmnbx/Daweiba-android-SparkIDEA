package com.dabawei.flashnote;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public final class QuickCaptureActivity extends Activity {
    private FlashNoteDatabase database;
    private EditText noteInput;
    private View rootLayout;
    private TextView title;
    private TextView subtitle;
    private Button saveButton;
    private ThemePalette currentTheme;

    private static final String PREFS_NAME = "dabawei_flashnote_prefs";
    private static final String PREF_THEME_KEY = "theme_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_capture);

        database = new FlashNoteDatabase(this);
        rootLayout = findViewById(R.id.quickRootLayout);
        noteInput = findViewById(R.id.quickNoteInput);
        title = findViewById(R.id.quickTitle);
        subtitle = findViewById(R.id.quickSubtitle);
        saveButton = findViewById(R.id.quickSaveButton);
        currentTheme = loadTheme();
        applyFontStyle(title, saveButton);
        applyTheme();
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveAndClose();
            }
        });

        focusInput();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (noteInput != null) {
            currentTheme = loadTheme();
            applyFontStyle(title, saveButton);
            applyTheme();
        }
    }

    private ThemePalette loadTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String stored = prefs.getString(PREF_THEME_KEY, "system");
        String migrated = ThemePalette.migratePreference(stored);
        if (!migrated.equals(stored)) {
            prefs.edit().putString(PREF_THEME_KEY, migrated).apply();
        }
        boolean systemIsDark = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        return ThemePalette.resolve(migrated, systemIsDark);
    }

    private void applyTheme() {
        int screen = Color.parseColor(currentTheme.getScreenColor());
        int input = Color.parseColor(currentTheme.getInputColor());
        int border = Color.parseColor(currentTheme.getBorderColor());
        int primary = Color.parseColor(currentTheme.getPrimaryTextColor());
        int secondary = Color.parseColor(currentTheme.getSecondaryTextColor());
        int action = Color.parseColor(currentTheme.getAccentDarkColor());
        rootLayout.setBackgroundColor(screen);
        title.setTextColor(primary);
        subtitle.setTextColor(secondary);
        noteInput.setTextColor(primary);
        noteInput.setHintTextColor(secondary);
        noteInput.setBackground(makeRoundedBackground(input, border, 12));
        saveButton.setBackground(makeRoundedBackground(action, Color.TRANSPARENT, 10));
        saveButton.setTextColor(Color.parseColor(currentTheme.getPrimaryButtonTextColor()));
        saveButton.setAllCaps(false);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            saveButton.setStateListAnimator(null);
            saveButton.setElevation(0f);
            getWindow().setStatusBarColor(screen);
            getWindow().setNavigationBarColor(screen);
            int flags = "dark".equals(currentTheme.getKey()) ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (android.os.Build.VERSION.SDK_INT >= 26 && !"dark".equals(currentTheme.getKey())) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    private GradientDrawable makeRoundedBackground(int fill, int stroke, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radiusDp * getResources().getDisplayMetrics().density);
        if (stroke != Color.TRANSPARENT) {
            drawable.setStroke(Math.max(1, Math.round(getResources().getDisplayMetrics().density)), stroke);
        }
        return drawable;
    }

    private void saveAndClose() {
        String content = noteInput.getText().toString().trim();
        if (content.length() == 0) {
            Toast.makeText(this, R.string.empty_note_tip, Toast.LENGTH_SHORT).show();
            return;
        }

        long createdAt = System.currentTimeMillis();
        database.insertNote(content, createdAt);
        FlashNoteWidgetProvider.refresh(this);
        finish();
    }

    private void focusInput() {
        noteInput.requestFocus();
        noteInput.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(noteInput, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }, 200);
    }

    private void applyFontStyle(TextView title, Button saveButton) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String style = UiFont.loadPreference(prefs);
        Typeface body = UiFont.body(this, style);
        Typeface medium = UiFont.medium(this, style);
        title.setTypeface(medium, Typeface.BOLD);
        noteInput.setTypeface(body, Typeface.NORMAL);
        saveButton.setTypeface(medium, Typeface.BOLD);
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }
}

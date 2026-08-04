package com.dabawei.flashnote;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
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

    private static final String PREFS_NAME = "dabawei_flashnote_prefs";
    private static final String PREF_CLAUDE_FONT_KEY = "claude_font_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_capture);

        database = new FlashNoteDatabase(this);
        noteInput = findViewById(R.id.quickNoteInput);
        TextView title = findViewById(R.id.quickTitle);
        Button saveButton = findViewById(R.id.quickSaveButton);
        applyFontStyle(title, saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveAndClose();
            }
        });

        focusInput();
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
        boolean useClaudeFont = prefs.getBoolean(PREF_CLAUDE_FONT_KEY, false);
        Typeface body = Typeface.create(useClaudeFont ? "serif" : "sans-serif", Typeface.NORMAL);
        Typeface medium = Typeface.create(useClaudeFont ? "serif" : "sans-serif-medium", Typeface.NORMAL);
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

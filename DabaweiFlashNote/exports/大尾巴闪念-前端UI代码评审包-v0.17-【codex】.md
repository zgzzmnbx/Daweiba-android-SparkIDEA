# 大尾巴闪念 前端/UI 代码评审包 v0.17

用途：把当前 Android 原生 UI 相关代码集中给其他 AI 或设计助手评审，重点看第一套“纸张”主题如何继续美化。

## 当前想改进的问题

- 主界面要更好看，更接近白底、深绿、轻卡片、时间流的精致笔记 App。
- 第一套主题是“纸张”，优先只改这一套。
- 主界面已有：打开即写、保存、保存为待办、同步、搜索、时间流、待办/已同步标签、删除图标。
- 记录卡片点击后已有菜单：编辑文字、重新同步该条、转换为待办/普通笔记、删除。
- 删除已有确认菜单。

## 技术约束

- 原生 Android Java + XML 布局。
- 当前不用 Compose、不用 Flutter、不用 React Native。
- 项目包名：`com.dabawei.flashnote`。
- 当前版本：`versionCode=17`，`versionName=0.17-paper-ui-note-actions`。

---

## `app\src\main\java\com\dabawei\flashnote\MainActivity.java`

```java
package com.dabawei.flashnote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.InputType;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private FlashNoteDatabase database;
    private EditText noteInput;
    private EditText searchInput;
    private NoteAdapter noteAdapter;
    private View rootLayout;
    private TextView appTitle;
    private TextView syncStatus;
    private TextView historyTitle;
    private Button saveButton;
    private Button saveTodoButton;
    private Button syncCurrentButton;
    private Button settingsButton;
    private ThemePalette currentTheme;

    private static final String PREFS_NAME = "dabawei_flashnote_prefs";
    private static final String PREF_THEME_KEY = "theme_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database = new FlashNoteDatabase(this);
        rootLayout = findViewById(R.id.rootLayout);
        appTitle = findViewById(R.id.appTitle);
        syncStatus = findViewById(R.id.syncStatus);
        historyTitle = findViewById(R.id.historyTitle);
        noteInput = findViewById(R.id.noteInput);
        searchInput = findViewById(R.id.searchInput);
        saveButton = findViewById(R.id.saveButton);
        saveTodoButton = findViewById(R.id.saveTodoButton);
        syncCurrentButton = findViewById(R.id.syncCurrentButton);
        settingsButton = findViewById(R.id.settingsButton);
        ListView noteList = findViewById(R.id.noteList);

        noteAdapter = new NoteAdapter(this);
        noteList.setAdapter(noteAdapter);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentTheme = ThemePalette.findByKey(prefs.getString(PREF_THEME_KEY, "paper"));
        applyTheme(currentTheme);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveCurrentNote();
            }
        });

        saveTodoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveCurrentTodo();
            }
        });

        syncCurrentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                syncPendingNotes();
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SyncSettingsActivity.class));
            }
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshNotes();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        refreshNotes();
        focusInput();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (noteAdapter != null) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            currentTheme = ThemePalette.findByKey(prefs.getString(PREF_THEME_KEY, "paper"));
            applyTheme(currentTheme);
            refreshNotes();
        }
    }

    private void saveCurrentNote() {
        saveCurrentInput(FlashNote.TYPE_NOTE);
    }

    private void saveCurrentTodo() {
        saveCurrentInput(FlashNote.TYPE_TODO);
    }

    private void saveCurrentInput(int noteType) {
        String content = noteInput.getText().toString().trim();
        if (content.length() == 0) {
            Toast.makeText(this, R.string.empty_note_tip, Toast.LENGTH_SHORT).show();
            return;
        }

        long createdAt = System.currentTimeMillis();
        database.insertNote(content, createdAt, noteType);
        noteInput.setText("");
        refreshNotes();
        FlashNoteWidgetProvider.refresh(this);
        focusInput();
    }

    private void refreshNotes() {
        String keyword = searchInput.getText().toString();
        noteAdapter.setNotes(database.searchNotes(keyword));
        int pendingCount = database.getPendingNotes().size();
        if (pendingCount == 0) {
            syncStatus.setText("● " + getString(R.string.synced_badge));
        } else {
            syncStatus.setText("● 待同步 " + pendingCount);
        }
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

    private void applyTheme(ThemePalette theme) {
        int screen = Color.parseColor(theme.getScreenColor());
        int surface = Color.parseColor(theme.getSurfaceColor());
        int input = Color.parseColor(theme.getInputColor());
        int primary = Color.parseColor(theme.getPrimaryTextColor());
        int secondary = Color.parseColor(theme.getSecondaryTextColor());
        int accent = Color.parseColor(theme.getAccentColor());
        int accentDark = Color.parseColor(theme.getAccentDarkColor());
        int border = Color.parseColor(theme.getBorderColor());
        int saveAction = Color.parseColor(theme.getSaveButtonColor());
        int todoAction = Color.parseColor(theme.getTodoButtonColor());
        int syncAction = Color.parseColor(theme.getSyncButtonColor());

        rootLayout.setBackgroundColor(screen);
        appTitle.setTextColor(primary);
        syncStatus.setTextColor(accent);
        historyTitle.setTextColor(secondary);

        styleField(searchInput, input, border, primary, secondary, 14);
        styleField(noteInput, input, border, primary, secondary, 14);
        styleButton(saveButton, saveAction, Color.WHITE, 14);
        styleButton(saveTodoButton, todoAction, Color.WHITE, 14);
        styleButton(syncCurrentButton, syncAction, Color.WHITE, 14);
        styleButton(settingsButton, surface, primary, 14);
        setElevationDp(searchInput, 2);
        setElevationDp(noteInput, 4);
        setElevationDp(saveButton, 3);
        setElevationDp(saveTodoButton, 2);
        setElevationDp(syncCurrentButton, 4);

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(screen);
            getWindow().setNavigationBarColor(screen);
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                int flags = "ink".equals(theme.getKey()) ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                getWindow().getDecorView().setSystemUiVisibility(flags);
            }
        }

        noteAdapter.setTheme(theme);
    }

    private void syncPendingNotes() {
        final SyncSettings settings = SyncSettings.load(this);
        final List<FlashNote> pending = database.getPendingNotes();
        if (pending.isEmpty()) {
            Toast.makeText(this, R.string.sync_no_pending, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, R.string.sync_queued, Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final WebDavMarkdownSync.Result result = new WebDavMarkdownSync().sync(pending, settings);
                if (result.isSynced()) {
                    database.markSyncState(pending, FlashNoteDatabase.SYNC_SYNCED);
                } else if (!result.isSkipped()) {
                    database.markSyncState(pending, FlashNoteDatabase.SYNC_FAILED);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshNotes();
                        if (result.isSynced()) {
                            Toast.makeText(MainActivity.this, getString(R.string.sync_batch_success, pending.size()), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MainActivity.this, getString(R.string.sync_failed, result.getMessage()), Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }).start();
    }

    private void styleField(EditText field, int backgroundColor, int borderColor, int textColor, int hintColor, int radiusDp) {
        field.setBackground(makeRoundedBackground(backgroundColor, borderColor, radiusDp));
        field.setTextColor(textColor);
        field.setHintTextColor(hintColor);
    }

    private void styleButton(Button button, int backgroundColor, int textColor, int radiusDp) {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            button.setBackgroundTintList(null);
        }
        button.setBackground(makeRoundedBackground(backgroundColor, Color.TRANSPARENT, radiusDp));
        button.setTextColor(textColor);
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

    private void setElevationDp(View view, float elevationDp) {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            view.setElevation(dp(elevationDp));
        }
    }

    private void showNoteActions(final FlashNote note) {
        final String toggleLabel = note.getNoteType() == FlashNote.TYPE_TODO
                ? getString(R.string.convert_to_note)
                : getString(R.string.convert_to_todo);
        String[] actions = new String[]{
                getString(R.string.edit_note),
                getString(R.string.resync_note),
                toggleLabel,
                getString(R.string.delete_note)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.note_actions_title)
                .setItems(actions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            showEditNoteDialog(note);
                        } else if (which == 1) {
                            resyncSingleNote(note);
                        } else if (which == 2) {
                            toggleNoteType(note);
                        } else if (which == 3) {
                            confirmDelete(note);
                        }
                    }
                })
                .show();
    }

    private void showEditNoteDialog(final FlashNote note) {
        final EditText editor = new EditText(this);
        editor.setMinLines(4);
        editor.setText(note.getContent());
        editor.setSelection(editor.getText().length());
        editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editor.setPadding((int) dp(14), (int) dp(12), (int) dp(14), (int) dp(12));
        styleField(editor,
                Color.parseColor(currentTheme.getInputColor()),
                Color.parseColor(currentTheme.getBorderColor()),
                Color.parseColor(currentTheme.getPrimaryTextColor()),
                Color.parseColor(currentTheme.getSecondaryTextColor()),
                12);

        new AlertDialog.Builder(this)
                .setTitle(R.string.edit_note)
                .setView(editor)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save_edit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String nextContent = editor.getText().toString().trim();
                        if (nextContent.length() == 0) {
                            Toast.makeText(MainActivity.this, R.string.empty_note_tip, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        database.updateNoteContent(note.getId(), nextContent);
                        refreshNotes();
                        FlashNoteWidgetProvider.refresh(MainActivity.this);
                        Toast.makeText(MainActivity.this, R.string.note_updated, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void toggleNoteType(FlashNote note) {
        int nextType = note.getNoteType() == FlashNote.TYPE_TODO ? FlashNote.TYPE_NOTE : FlashNote.TYPE_TODO;
        database.updateNoteType(note.getId(), nextType);
        refreshNotes();
        FlashNoteWidgetProvider.refresh(this);
        Toast.makeText(this, R.string.note_type_updated, Toast.LENGTH_SHORT).show();
    }

    private void confirmDelete(final FlashNote note) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete_note, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        database.deleteNote(note.getId());
                        refreshNotes();
                        FlashNoteWidgetProvider.refresh(MainActivity.this);
                        Toast.makeText(MainActivity.this, R.string.note_deleted, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void resyncSingleNote(final FlashNote note) {
        final SyncSettings settings = SyncSettings.load(this);
        Toast.makeText(this, R.string.sync_queued, Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final WebDavMarkdownSync.Result result = new WebDavMarkdownSync().sync(Collections.singletonList(note), settings);
                if (result.isSynced()) {
                    database.markSyncState(note.getId(), FlashNoteDatabase.SYNC_SYNCED);
                } else if (!result.isSkipped()) {
                    database.markSyncState(note.getId(), FlashNoteDatabase.SYNC_FAILED);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshNotes();
                        if (result.isSynced()) {
                            Toast.makeText(MainActivity.this, R.string.note_resync_success, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MainActivity.this, getString(R.string.sync_failed, result.getMessage()), Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }

    private final class NoteAdapter extends BaseAdapter {
        private final LayoutInflater inflater;
        private final SimpleDateFormat dateTimeFormat;
        private final List<FlashNote> notes = new ArrayList<>();
        private ThemePalette theme = ThemePalette.findByKey("paper");

        NoteAdapter(Context context) {
            inflater = LayoutInflater.from(context);
            dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        }

        void setNotes(List<FlashNote> nextNotes) {
            notes.clear();
            notes.addAll(nextNotes);
            notifyDataSetChanged();
        }

        void setTheme(ThemePalette nextTheme) {
            theme = nextTheme;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return notes.size();
        }

        @Override
        public Object getItem(int position) {
            return notes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return notes.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            ViewHolder holder;
            if (row == null) {
                row = inflater.inflate(R.layout.note_item, parent, false);
                holder = new ViewHolder();
                holder.noteCard = row.findViewById(R.id.noteCard);
                holder.timelineLine = row.findViewById(R.id.timelineLine);
                holder.timelineDot = row.findViewById(R.id.timelineDot);
                holder.content = row.findViewById(R.id.noteContent);
                holder.time = row.findViewById(R.id.noteTime);
                holder.syncBadge = row.findViewById(R.id.syncBadge);
                holder.todoBadge = row.findViewById(R.id.todoBadge);
                holder.deleteButton = row.findViewById(R.id.deleteButton);
                row.setTag(holder);
            } else {
                holder = (ViewHolder) row.getTag();
            }

            FlashNote note = notes.get(position);
            holder.content.setTextColor(Color.parseColor(theme.getPrimaryTextColor()));
            holder.time.setTextColor(Color.parseColor(theme.getSecondaryTextColor()));
            holder.noteCard.setBackground(makeRoundedBackground(
                    Color.parseColor(theme.getSurfaceColor()),
                    Color.parseColor(theme.getBorderColor()),
                    "paper".equals(theme.getKey()) ? 12 : 8));
            holder.timelineLine.setBackgroundColor(Color.parseColor("paper".equals(theme.getKey()) ? "#D6DDD7" : theme.getBorderColor()));
            holder.timelineDot.setBackground(makeRoundedBackground(Color.parseColor(theme.getAccentDarkColor()), Color.TRANSPARENT, 8));
            setElevationDp(holder.noteCard, "paper".equals(theme.getKey()) ? 2 : 0);
            holder.content.setText(note.getContent());
            holder.time.setText(dateTimeFormat.format(new Date(note.getCreatedAtMillis())));
            holder.syncBadge.setVisibility(note.getSyncState() == FlashNoteDatabase.SYNC_SYNCED ? View.VISIBLE : View.GONE);
            holder.syncBadge.setTextColor(Color.parseColor(theme.getAccentDarkColor()));
            holder.todoBadge.setVisibility(note.getNoteType() == FlashNote.TYPE_TODO ? View.VISIBLE : View.GONE);
            holder.deleteButton.setBackground(makeRoundedBackground(
                    Color.TRANSPARENT,
                    Color.parseColor(theme.getBorderColor()),
                    8));
            holder.noteCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showNoteActions(note);
                }
            });
            holder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    confirmDelete(note);
                }
            });
            return row;
        }
    }

    private static final class ViewHolder {
        View noteCard;
        View timelineLine;
        View timelineDot;
        TextView content;
        TextView time;
        TextView syncBadge;
        TextView todoBadge;
        ImageButton deleteButton;
    }
}

```

---

## `app\src\main\java\com\dabawei\flashnote\ThemePalette.java`

```java
package com.dabawei.flashnote;

public final class ThemePalette {
    private static final ThemePalette PAPER = new ThemePalette(
            "paper", "纸张", "#FBFAF7", "#FFFFFF", "#1F2328", "#8B9097",
            "#24724D", "#185C3D", "#E2E4E2", "#FFFFFF");
    private static final ThemePalette INK = new ThemePalette(
            "ink", "夜墨", "#111614", "#1B2320", "#EEF4EE", "#A9B5AE",
            "#8FCBA9", "#5EA17D", "#34413B", "#18201D");
    private static final ThemePalette FOREST = new ThemePalette(
            "forest", "森绿", "#E6EFE7", "#F9FCF8", "#10251B", "#60736A",
            "#2E7D56", "#1E5B3F", "#B9CDBF", "#EEF6ED");
    private static final ThemePalette APPLE = new ThemePalette(
            "apple", "Apple", "#F5F5F7", "#FFFFFF", "#1D1D1F", "#7A7A7A",
            "#0066CC", "#0057B8", "#E0E0E0", "#FAFAFC");
    private static final ThemePalette LINEAR = new ThemePalette(
            "linear", "Linear", "#010102", "#0F1011", "#F7F8F8", "#8A8F98",
            "#5E6AD2", "#4D58B8", "#34343A", "#141516");
    private static final ThemePalette NOTION = new ThemePalette(
            "notion", "Notion", "#F6F5F4", "#FFFFFF", "#37352F", "#787671",
            "#5645D4", "#4534B3", "#E5E3DF", "#FAFAF9");
    private static final ThemePalette RAYCAST = new ThemePalette(
            "raycast", "Raycast", "#18191D", "#22242A", "#F3F4F7", "#A7ABB5",
            "#FF6363", "#E54D4D", "#3A3D46", "#202228");
    private static final ThemePalette OBSIDIAN = new ThemePalette(
            "obsidian", "Obsidian", "#1E1B2E", "#28243A", "#F2ECFF", "#B7A9D6",
            "#8B5CF6", "#6D42D9", "#463B62", "#241F34");

    private static final ThemePalette[] THEMES = new ThemePalette[]{
            PAPER, INK, FOREST, APPLE, LINEAR, NOTION, RAYCAST, OBSIDIAN};

    private final String key;
    private final String label;
    private final String screenColor;
    private final String surfaceColor;
    private final String primaryTextColor;
    private final String secondaryTextColor;
    private final String accentColor;
    private final String accentDarkColor;
    private final String borderColor;
    private final String inputColor;

    private ThemePalette(
            String key,
            String label,
            String screenColor,
            String surfaceColor,
            String primaryTextColor,
            String secondaryTextColor,
            String accentColor,
            String accentDarkColor,
            String borderColor,
            String inputColor) {
        this.key = key;
        this.label = label;
        this.screenColor = screenColor;
        this.surfaceColor = surfaceColor;
        this.primaryTextColor = primaryTextColor;
        this.secondaryTextColor = secondaryTextColor;
        this.accentColor = accentColor;
        this.accentDarkColor = accentDarkColor;
        this.borderColor = borderColor;
        this.inputColor = inputColor;
    }

    public static ThemePalette[] all() {
        return THEMES.clone();
    }

    public static ThemePalette findByKey(String key) {
        for (ThemePalette theme : THEMES) {
            if (theme.key.equals(key)) {
                return theme;
            }
        }
        return PAPER;
    }

    public static ThemePalette next(String key) {
        ThemePalette current = findByKey(key);
        for (int i = 0; i < THEMES.length; i++) {
            if (THEMES[i].key.equals(current.key)) {
                return THEMES[(i + 1) % THEMES.length];
            }
        }
        return PAPER;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public String getScreenColor() {
        return screenColor;
    }

    public String getSurfaceColor() {
        return surfaceColor;
    }

    public String getPrimaryTextColor() {
        return primaryTextColor;
    }

    public String getSecondaryTextColor() {
        return secondaryTextColor;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public String getAccentDarkColor() {
        return accentDarkColor;
    }

    public String getBorderColor() {
        return borderColor;
    }

    public String getInputColor() {
        return inputColor;
    }

    public String getSaveButtonColor() {
        return accentDarkColor;
    }

    public String getTodoButtonColor() {
        switch (key) {
            case "paper":
                return "#1E5AA8";
            case "ink":
                return "#7E9DE8";
            case "forest":
                return "#3B7A65";
            case "apple":
                return "#007AFF";
            case "linear":
                return "#5E6AD2";
            case "notion":
                return "#9B6A32";
            case "raycast":
                return "#FF8A65";
            case "obsidian":
                return "#A78BFA";
            default:
                return accentColor;
        }
    }

    public String getSyncButtonColor() {
        switch (key) {
            case "paper":
                return "#236A4B";
            case "ink":
                return "#5EA17D";
            case "forest":
                return "#1E5B3F";
            case "apple":
                return "#34C759";
            case "linear":
                return "#26A269";
            case "notion":
                return "#0F7B6C";
            case "raycast":
                return "#E54D4D";
            case "obsidian":
                return "#7C3AED";
            default:
                return accentDarkColor;
        }
    }
}

```

---

## `app\src\main\java\com\dabawei\flashnote\SyncSettingsActivity.java`

```java
package com.dabawei.flashnote;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
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
    private FlashNoteDatabase database;
    private Spinner themeSpinner;
    private Button exportButton;
    private ThemePalette currentTheme;

    private static final String THEME_PREFS_NAME = "dabawei_flashnote_prefs";
    private static final String PREF_THEME_KEY = "theme_key";

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
        themeSpinner = findViewById(R.id.themeSpinner);
        exportButton = findViewById(R.id.exportButton);
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
        bindThemeSpinner();

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

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }
}

```

---

## `app\src\main\res\layout\activity_main.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/rootLayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/screen_background"
    android:orientation="vertical"
    android:paddingLeft="18dp"
    android:paddingTop="34dp"
    android:paddingRight="18dp"
    android:paddingBottom="18dp">

    <LinearLayout
        android:id="@+id/titleBar"
        android:layout_width="match_parent"
        android:layout_height="72dp"
        android:gravity="center_vertical"
        android:orientation="horizontal">

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:gravity="center_vertical"
            android:orientation="vertical">

            <TextView
                android:id="@+id/appTitle"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/app_name"
                android:textColor="@color/primary_text"
                android:textSize="30sp"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/syncStatus"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="6dp"
                android:text="@string/synced_badge"
                android:textColor="@color/accent_green"
                android:textSize="15sp"
                android:textStyle="bold" />
        </LinearLayout>

        <Button
            android:id="@+id/syncCurrentButton"
            android:layout_width="wrap_content"
            android:layout_height="52dp"
            android:layout_marginRight="8dp"
            android:minWidth="88dp"
            android:text="@string/sync_settings"
            android:textColor="@android:color/white"
            android:textSize="16sp"
            android:textStyle="bold" />

        <Button
            android:id="@+id/settingsButton"
            android:layout_width="42dp"
            android:layout_height="42dp"
            android:minWidth="42dp"
            android:text="@string/settings_gear"
            android:textSize="20sp" />
    </LinearLayout>

    <EditText
        android:id="@+id/searchInput"
        android:layout_width="match_parent"
        android:layout_height="54dp"
        android:layout_marginTop="12dp"
        android:background="@drawable/input_background"
        android:hint="@string/search_hint"
        android:imeOptions="actionSearch"
        android:inputType="text"
        android:paddingLeft="14dp"
        android:paddingRight="14dp"
        android:singleLine="true"
        android:textColor="@color/primary_text"
        android:textColorHint="@color/secondary_text"
        android:textSize="17sp" />

    <EditText
        android:id="@+id/noteInput"
        android:layout_width="match_parent"
        android:layout_height="150dp"
        android:layout_marginTop="18dp"
        android:background="@drawable/input_background"
        android:gravity="top|start"
        android:hint="@string/note_hint"
        android:imeOptions="flagNoExtractUi"
        android:inputType="textMultiLine|textCapSentences"
        android:minLines="4"
        android:padding="14dp"
        android:textColor="@color/primary_text"
        android:textColorHint="@color/secondary_text"
        android:textSize="21sp" />

    <LinearLayout
        android:id="@+id/saveActions"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="14dp"
        android:orientation="horizontal">

        <Button
            android:id="@+id/saveButton"
            android:layout_width="0dp"
            android:layout_height="56dp"
            android:layout_marginRight="8dp"
            android:layout_weight="1"
            android:text="@string/save_note"
            android:textColor="@android:color/white"
            android:textSize="18sp"
            android:textStyle="bold" />

        <Button
            android:id="@+id/saveTodoButton"
            android:layout_width="0dp"
            android:layout_height="56dp"
            android:layout_weight="1"
            android:text="@string/save_todo"
            android:textColor="@android:color/white"
            android:textSize="16sp"
            android:textStyle="bold" />
    </LinearLayout>

    <TextView
        android:id="@+id/historyTitle"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="22dp"
        android:text="@string/history_title"
        android:textColor="@color/secondary_text"
        android:textSize="14sp"
        android:textStyle="bold" />

    <ListView
        android:id="@+id/noteList"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_marginTop="8dp"
        android:layout_weight="1"
        android:cacheColorHint="@android:color/transparent"
        android:divider="@android:color/transparent"
        android:dividerHeight="8dp"
        android:scrollbarStyle="outsideOverlay" />
</LinearLayout>

```

---

## `app\src\main\res\layout\note_item.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:paddingLeft="2dp"
    android:paddingRight="0dp">

    <FrameLayout
        android:layout_width="30dp"
        android:layout_height="match_parent">

        <View
            android:id="@+id/timelineLine"
            android:layout_width="1dp"
            android:layout_height="match_parent"
            android:layout_gravity="center_horizontal"
            android:background="#D6DDD7" />

        <TextView
            android:id="@+id/timelineDot"
            android:layout_width="14dp"
            android:layout_height="14dp"
            android:layout_gravity="center_horizontal"
            android:layout_marginTop="24dp"
            android:background="@drawable/todo_badge_background" />
    </FrameLayout>

    <LinearLayout
        android:id="@+id/noteCard"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginBottom="10dp"
        android:layout_weight="1"
        android:background="@drawable/note_item_background"
        android:clickable="true"
        android:focusable="true"
        android:orientation="vertical"
        android:padding="14dp">

    <TextView
        android:id="@+id/noteContent"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:lineSpacingExtra="2dp"
        android:textColor="@color/primary_text"
        android:textSize="18sp" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:gravity="center_vertical"
        android:orientation="horizontal">

        <TextView
            android:id="@+id/noteTime"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:textColor="@color/secondary_text"
            android:textSize="12sp" />

        <TextView
            android:id="@+id/todoBadge"
            android:layout_width="wrap_content"
            android:layout_height="24dp"
            android:background="@drawable/todo_badge_background"
            android:gravity="center"
            android:paddingLeft="8dp"
            android:paddingRight="8dp"
            android:text="@string/todo_badge"
            android:textColor="@android:color/white"
            android:textSize="11sp" />

        <TextView
            android:id="@+id/syncBadge"
            android:layout_width="wrap_content"
            android:layout_height="24dp"
            android:layout_marginLeft="6dp"
            android:background="@drawable/synced_badge_background"
            android:gravity="center"
            android:paddingLeft="8dp"
            android:paddingRight="8dp"
            android:text="@string/synced_badge"
            android:textColor="@android:color/white"
            android:textSize="11sp" />

        <ImageButton
            android:id="@+id/deleteButton"
            android:layout_width="34dp"
            android:layout_height="34dp"
            android:layout_marginLeft="8dp"
            android:background="@drawable/input_background"
            android:contentDescription="@string/delete_note"
            android:padding="7dp"
            android:scaleType="center"
            android:src="@drawable/ic_trash" />
    </LinearLayout>
    </LinearLayout>
</LinearLayout>

```

---

## `app\src\main\res\layout\activity_sync_settings.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/screen_background">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:paddingLeft="18dp"
        android:paddingTop="34dp"
        android:paddingRight="18dp"
        android:paddingBottom="18dp">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="54dp"
            android:gravity="center_vertical"
            android:text="@string/settings_title"
            android:textColor="@color/primary_text"
            android:textSize="25sp"
            android:textStyle="bold" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="28dp"
            android:layout_marginTop="8dp"
            android:gravity="bottom"
            android:text="@string/theme_select"
            android:textColor="@color/secondary_text"
            android:textSize="14sp" />

        <Spinner
            android:id="@+id/themeSpinner"
            android:layout_width="match_parent"
            android:layout_height="52dp"
            android:layout_marginTop="6dp"
            android:background="@drawable/input_background"
            android:paddingLeft="12dp"
            android:paddingRight="12dp" />

        <Button
            android:id="@+id/exportButton"
            android:layout_width="match_parent"
            android:layout_height="52dp"
            android:layout_marginTop="10dp"
            android:backgroundTint="@color/accent_green"
            android:text="@string/export_markdown"
            android:textColor="@android:color/white"
            android:textSize="17sp"
            android:textStyle="bold" />

        <CheckBox
            android:id="@+id/syncEnabled"
            android:layout_width="match_parent"
            android:layout_height="48dp"
            android:text="@string/sync_enabled"
            android:textColor="@color/primary_text"
            android:textSize="16sp" />

        <EditText
            android:id="@+id/webdavBaseUrl"
            android:layout_width="match_parent"
            android:layout_height="52dp"
            android:layout_marginTop="10dp"
            android:background="@drawable/input_background"
            android:hint="@string/webdav_base_url"
            android:inputType="textUri"
            android:padding="12dp"
            android:singleLine="true"
            android:textColor="@color/primary_text"
            android:textColorHint="@color/secondary_text" />

        <EditText
            android:id="@+id/webdavUsername"
            android:layout_width="match_parent"
            android:layout_height="52dp"
            android:layout_marginTop="10dp"
            android:background="@drawable/input_background"
            android:hint="@string/webdav_username"
            android:inputType="textEmailAddress"
            android:padding="12dp"
            android:singleLine="true"
            android:textColor="@color/primary_text"
            android:textColorHint="@color/secondary_text" />

        <EditText
            android:id="@+id/webdavPassword"
            android:layout_width="match_parent"
            android:layout_height="52dp"
            android:layout_marginTop="10dp"
            android:background="@drawable/input_background"
            android:hint="@string/webdav_password"
            android:inputType="textPassword"
            android:padding="12dp"
            android:singleLine="true"
            android:textColor="@color/primary_text"
            android:textColorHint="@color/secondary_text" />

        <EditText
            android:id="@+id/webdavRemotePath"
            android:layout_width="match_parent"
            android:layout_height="52dp"
            android:layout_marginTop="10dp"
            android:background="@drawable/input_background"
            android:hint="@string/webdav_remote_path"
            android:inputType="textUri"
            android:padding="12dp"
            android:singleLine="true"
            android:textColor="@color/primary_text"
            android:textColorHint="@color/secondary_text" />

        <EditText
            android:id="@+id/webdavAnchor"
            android:layout_width="match_parent"
            android:layout_height="52dp"
            android:layout_marginTop="10dp"
            android:background="@drawable/input_background"
            android:hint="@string/webdav_anchor"
            android:inputType="text"
            android:padding="12dp"
            android:singleLine="true"
            android:textColor="@color/primary_text"
            android:textColorHint="@color/secondary_text" />

        <Button
            android:id="@+id/saveSyncSettingsButton"
            android:layout_width="match_parent"
            android:layout_height="52dp"
            android:layout_marginTop="16dp"
            android:backgroundTint="@color/accent_green_dark"
            android:text="@string/save_settings"
            android:textColor="@android:color/white"
            android:textSize="18sp"
            android:textStyle="bold" />
    </LinearLayout>
</ScrollView>

```

---

## `app\src\main\res\values\strings.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">大尾巴闪念</string>
    <string name="search_hint">搜索闪念</string>
    <string name="note_hint">想到什么，马上写下</string>
    <string name="save_note">保存</string>
    <string name="save_todo">保存为待办</string>
    <string name="todo_badge">待办</string>
    <string name="export_markdown">导出</string>
    <string name="theme_switch">主题</string>
    <string name="theme_select">主题选择</string>
    <string name="theme_button_label">主题：%1$s</string>
    <string name="settings_title">设置</string>
    <string name="settings_gear">⚙</string>
    <string name="synced_badge">已同步</string>
    <string name="delete_note">删除</string>
    <string name="note_deleted">已删除</string>
    <string name="empty_note_tip">先写一点内容</string>
    <string name="export_success">已导出：%1$s</string>
    <string name="export_failed">导出失败：%1$s</string>
    <string name="history_title">时间流</string>
    <string name="widget_title">大尾巴闪念</string>
    <string name="widget_action">写一条闪念</string>
    <string name="widget_recent_empty">还没有闪念</string>
    <string name="quick_capture_title">快速闪念</string>
    <string name="sync_settings">同步</string>
    <string name="record_action">＋ 记录闪念</string>
    <string name="note_actions_title">记录操作</string>
    <string name="edit_note">编辑文字</string>
    <string name="resync_note">重新同步该条</string>
    <string name="convert_to_todo">转换为待办</string>
    <string name="convert_to_note">转换为普通笔记</string>
    <string name="confirm_delete_title">删除这条记录？</string>
    <string name="confirm_delete_message">删除后只会从手机本地移除，不会自动删除已经同步到 Obsidian 的内容。</string>
    <string name="cancel">取消</string>
    <string name="save_edit">保存修改</string>
    <string name="note_updated">已更新，等待重新同步</string>
    <string name="note_type_updated">已转换，等待重新同步</string>
    <string name="note_resync_success">已重新同步该条</string>
    <string name="sync_enabled">启用 WebDAV 同步</string>
    <string name="webdav_base_url">WebDAV 地址，如 https://dav.jianguoyun.com/dav</string>
    <string name="webdav_username">坚果云账号</string>
    <string name="webdav_password">WebDAV 应用密码</string>
    <string name="webdav_remote_path">目标 Markdown 路径，如 /我的坚果云/inbox.md</string>
    <string name="webdav_anchor">插入锚点</string>
    <string name="save_settings">保存设置</string>
    <string name="sync_pending_now">立即同步待同步</string>
    <string name="settings_saved">同步设置已保存</string>
    <string name="sync_no_pending">没有待同步闪念</string>
    <string name="sync_batch_success">已同步 %1$d 条闪念</string>
    <string name="sync_queued">已保存，本条将尝试同步</string>
    <string name="sync_failed">已本地保存，同步失败：%1$s</string>
</resources>

```

---

## `app\src\main\res\values\colors.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="screen_background">#F7F4EE</color>
    <color name="surface_background">#FFFFFF</color>
    <color name="primary_text">#18211F</color>
    <color name="secondary_text">#6B706D</color>
    <color name="accent_green">#1C7C54</color>
    <color name="accent_green_dark">#145C3E</color>
    <color name="field_border">#D8D2C6</color>
    <color name="business_blue">#1E5AA8</color>
</resources>

```

---

## `app\src\main\res\drawable\input_background.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/surface_background" />
    <stroke
        android:width="1dp"
        android:color="@color/field_border" />
    <corners android:radius="8dp" />
</shape>

```

---

## `app\src\main\res\drawable\note_item_background.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/surface_background" />
    <stroke
        android:width="1dp"
        android:color="@color/field_border" />
    <corners android:radius="8dp" />
</shape>

```

---

## `app\src\main\res\drawable\todo_badge_background.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/business_blue" />
    <corners android:radius="8dp" />
</shape>

```

---

## `app\src\main\res\drawable\synced_badge_background.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#EAF5EF" />
    <stroke
        android:width="1dp"
        android:color="#6FAF8B" />
    <corners android:radius="8dp" />
</shape>

```

---

## `app\src\main\res\drawable\ic_trash.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="22dp"
    android:height="22dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#00000000"
        android:pathData="M3,6h18"
        android:strokeColor="#6B706D"
        android:strokeLineCap="round"
        android:strokeWidth="2" />
    <path
        android:fillColor="#00000000"
        android:pathData="M8,6V4h8v2"
        android:strokeColor="#6B706D"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:strokeWidth="2" />
    <path
        android:fillColor="#00000000"
        android:pathData="M6,6l1,15h10l1,-15"
        android:strokeColor="#6B706D"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:strokeWidth="2" />
    <path
        android:fillColor="#00000000"
        android:pathData="M10,10v7M14,10v7"
        android:strokeColor="#6B706D"
        android:strokeLineCap="round"
        android:strokeWidth="2" />
</vector>

```


package com.dabawei.flashnote;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.content.Context;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private FlashNoteDatabase database;
    private EditText noteInput;
    private EditText searchInput;
    private NoteAdapter noteAdapter;
    private View pullRoot;
    private View rootLayout;
    private View pullIndicator;
    private ImageView pullIndicatorIcon;
    private TextView pullIndicatorText;
    private View searchPanel;
    private View noteInputPanel;
    private View saveActions;
    private View historyHeader;
    private View todoPage;
    private View bottomNav;
    private ListView noteList;
    private ListView todoList;
    private TextView appTitle;
    private TextView syncStatus;
    private TextView historyTitle;
    private TextView todoPageTitle;
    private TextView todoEmptyTitle;
    private TextView todoEmptyMessage;
    private TextView searchIcon;
    private View navHome;
    private View navTags;
    private View navStats;
    private View navMine;
    private ImageView navHomeIcon;
    private ImageView navTagsIcon;
    private ImageView navStatsIcon;
    private ImageView navMineIcon;
    private TextView navHomeLabel;
    private TextView navTagsLabel;
    private TextView navStatsLabel;
    private TextView navMineLabel;
    private Button saveButton;
    private Button saveTodoButton;
    private ImageButton uploadImageButton;
    private ImageButton clipboardButton;
    private Button recordButton;
    private ThemePalette currentTheme;
    private TodoAdapter todoAdapter;
    private ReminderScheduler reminderScheduler;
    private boolean claudeFontEnabled;
    private int currentPage = PAGE_HOME;
    private boolean todoSyncing;
    private boolean pendingBatchSyncAfterPermission;
    private FlashNote pendingSingleSyncAfterPermission;
    private String highlightedTaskId;
    private final Set<String> autoConflictTaskIds = new HashSet<>();
    private boolean exactAlarmPromptAfterNotification;
    private float pullStartY;
    private boolean isPulling;

    private static final int PAGE_HOME = 0;
    private static final int PAGE_HISTORY = 1;
    private static final int PAGE_TODO = 2;
    private static final String PREFS_NAME = "dabawei_flashnote_prefs";
    private static final String PREF_THEME_KEY = "theme_key";
    private static final String PREF_CLAUDE_FONT_KEY = "claude_font_enabled";
    private static final String PENDING_IMAGE_DIR = "pending-images";
    private static final int REQUEST_PICK_IMAGE = 1001;
    private static final int REQUEST_READ_IMAGES_PERMISSION = 1002;
    private static final int REQUEST_POST_NOTIFICATIONS = 1003;
    private static final float PULL_TRIGGER_DP = 112f;
    private static final float PULL_MAX_DP = 142f;
    private static final Pattern PENDING_IMAGE_PATTERN = Pattern.compile("!\\[待上传图片\\]\\(((?:content|file):[^)]+)\\)");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database = new FlashNoteDatabase(this);
        reminderScheduler = new ReminderScheduler(this, database);
        reminderScheduler.rescheduleAll();
        BackgroundSyncScheduler.ensureScheduled(this);
        pullRoot = findViewById(R.id.pullRoot);
        rootLayout = findViewById(R.id.rootLayout);
        pullIndicator = findViewById(R.id.pullIndicator);
        pullIndicatorIcon = findViewById(R.id.pullIndicatorIcon);
        pullIndicatorText = findViewById(R.id.pullIndicatorText);
        searchPanel = findViewById(R.id.searchPanel);
        noteInputPanel = findViewById(R.id.noteInputPanel);
        saveActions = findViewById(R.id.saveActions);
        historyHeader = findViewById(R.id.historyHeader);
        todoPage = findViewById(R.id.todoPage);
        bottomNav = findViewById(R.id.bottomNav);
        appTitle = findViewById(R.id.appTitle);
        syncStatus = findViewById(R.id.syncStatus);
        historyTitle = findViewById(R.id.historyTitle);
        todoPageTitle = findViewById(R.id.todoPageTitle);
        todoEmptyTitle = findViewById(R.id.todoEmptyTitle);
        todoEmptyMessage = findViewById(R.id.todoEmptyMessage);
        searchIcon = findViewById(R.id.searchIcon);
        navHome = findViewById(R.id.navHome);
        navTags = findViewById(R.id.navTags);
        navStats = findViewById(R.id.navStats);
        navMine = findViewById(R.id.navMine);
        navHomeIcon = findViewById(R.id.navHomeIcon);
        navTagsIcon = findViewById(R.id.navTagsIcon);
        navStatsIcon = findViewById(R.id.navStatsIcon);
        navMineIcon = findViewById(R.id.navMineIcon);
        navHomeLabel = findViewById(R.id.navHomeLabel);
        navTagsLabel = findViewById(R.id.navTagsLabel);
        navStatsLabel = findViewById(R.id.navStatsLabel);
        navMineLabel = findViewById(R.id.navMineLabel);
        noteInput = findViewById(R.id.noteInput);
        searchInput = findViewById(R.id.searchInput);
        saveButton = findViewById(R.id.saveButton);
        saveTodoButton = findViewById(R.id.saveTodoButton);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        clipboardButton = findViewById(R.id.clipboardButton);
        recordButton = findViewById(R.id.recordButton);
        noteList = findViewById(R.id.noteList);
        todoList = findViewById(R.id.todoList);

        noteAdapter = new NoteAdapter(this);
        noteList.setAdapter(noteAdapter);
        todoAdapter = new TodoAdapter(this);
        todoList.setAdapter(todoAdapter);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentTheme = ThemePalette.findByKey(prefs.getString(PREF_THEME_KEY, "paper"));
        claudeFontEnabled = prefs.getBoolean(PREF_CLAUDE_FONT_KEY, false);
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

        uploadImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickImage();
            }
        });

        clipboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pasteLatestClipboardText();
            }
        });

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                syncPendingNotes();
            }
        });

        navHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchPage(PAGE_HOME);
            }
        });

        navTags.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchPage(PAGE_HISTORY);
            }
        });

        navStats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchPage(PAGE_TODO);
            }
        });

        navMine.setOnClickListener(new View.OnClickListener() {
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
        bindPullGesture();
        switchPage(PAGE_HOME);

        refreshNotes();
        focusInput();
        handleIncomingShare(getIntent());
        handleReminderIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (noteAdapter != null) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            currentTheme = ThemePalette.findByKey(prefs.getString(PREF_THEME_KEY, "paper"));
            claudeFontEnabled = prefs.getBoolean(PREF_CLAUDE_FONT_KEY, false);
            applyTheme(currentTheme);
            refreshNotes();
            if (reminderScheduler != null) {
                reminderScheduler.rescheduleAll();
            }
            BackgroundSyncScheduler.ensureScheduled(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingShare(intent);
        handleReminderIntent(intent);
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
        long noteId = database.insertNote(content, createdAt, noteType);
        noteInput.setText("");
        refreshNotes();
        FlashNoteWidgetProvider.refresh(this);
        focusInput();
        if (noteType == FlashNote.TYPE_TODO) {
            final FlashNote savedTodo = new FlashNote(
                    noteId,
                    content,
                    createdAt,
                    FlashNoteDatabase.SYNC_PENDING,
                    FlashNote.TYPE_TODO);
            final NaturalLanguageReminderParser.ParseResult parsed =
                    NaturalLanguageReminderParser.parseResult(content, createdAt);
            if (parsed.isAutoEligible()) {
                saveAutomaticReminder(ReminderTarget.forLocalNote(savedTodo), parsed);
                return;
            }
            final boolean conflict = parsed.getCandidates().size() > 1;
            new AlertDialog.Builder(this)
                    .setTitle(conflict ? R.string.p1_natural_time_title : R.string.todo_badge)
                    .setMessage(conflict
                            ? R.string.p1_natural_time_conflict
                            : R.string.reminder_add)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.reminder_add, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            showReminderPicker(ReminderTarget.forLocalNote(savedTodo));
                        }
                    })
                    .show();
        }
    }

    private void syncTodoItems() {
        if (todoSyncing) {
            return;
        }
        final SyncSettings settings = SyncSettings.load(this);
        if (!settings.isReady()) {
            Toast.makeText(this, R.string.image_sync_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }
        todoSyncing = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                final TodoSyncCoordinator.SyncResult result = TodoSyncCoordinator.sync(
                        MainActivity.this,
                        database,
                        reminderScheduler);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        todoSyncing = false;
                        if (result.isSuccess()) {
                            autoConflictTaskIds.clear();
                            if (result.getSummary() != null) {
                                autoConflictTaskIds.addAll(result.getSummary().getConflictTaskIds());
                            }
                            showTodoItems(result.getItems());
                            String successMessage = getString(R.string.todo_sync_success, result.getItems().size());
                            if (result.getSummary() != null && result.getSummary().getOverdueCount() > 0) {
                                successMessage += "；" + getString(R.string.reminder_overdue);
                            }
                            if (result.getSummary() != null && result.getSummary().getConflictCount() > 0) {
                                successMessage += "；" + getString(R.string.p1_natural_time_conflict);
                            }
                            Toast.makeText(
                                    MainActivity.this,
                                    successMessage,
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(
                                    MainActivity.this,
                                    getString(R.string.todo_sync_failed, result.getMessage()),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }).start();
    }

    private void showTodoItems(List<TodoSyncItem> items) {
        todoAdapter.setItems(items == null ? Collections.<TodoSyncItem>emptyList() : items);
        boolean hasItems = items != null && !items.isEmpty();
        todoList.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        todoEmptyTitle.setVisibility(hasItems ? View.GONE : View.VISIBLE);
        todoEmptyMessage.setVisibility(hasItems ? View.GONE : View.VISIBLE);
        if (highlightedTaskId != null && highlightedTaskId.length() > 0) {
            final int position = todoAdapter.findPositionByTaskId(highlightedTaskId);
            if (position >= 0) {
                todoList.post(new Runnable() {
                    @Override
                    public void run() {
                        todoList.setSelection(position);
                    }
                });
            }
            highlightedTaskId = null;
        }
    }

    private void handleReminderIntent(Intent intent) {
        if (intent == null || !intent.getBooleanExtra(ReminderScheduler.EXTRA_OPEN_TODO, false)) {
            return;
        }
        highlightedTaskId = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_ID);
        switchPage(PAGE_TODO);
    }

    private void showReminderPicker(final ReminderTarget target) {
        if (target == null || target.taskId.length() == 0) {
            return;
        }
        target.existingReminder = database.getReminderByTaskId(target.taskId);
        final ArrayList<String> labels = new ArrayList<>();
        final ArrayList<Integer> actions = new ArrayList<>();
        labels.add(getString(R.string.reminder_one_hour));
        actions.add(ReminderAction.ONE_HOUR);
        long now = System.currentTimeMillis();
        long todayAtSix = ReminderTimeCalculator.todayAt(now, 18, 0);
        if (todayAtSix > 0L) {
            labels.add(getString(R.string.reminder_today_18));
            actions.add(ReminderAction.TODAY_AT_SIX);
        }
        labels.add(getString(R.string.reminder_tomorrow_9));
        actions.add(ReminderAction.TOMORROW_AT_NINE);
        labels.add(getString(R.string.reminder_custom));
        actions.add(ReminderAction.CUSTOM);
        labels.add(getString(R.string.p1_multi_reminder));
        actions.add(ReminderAction.PRE_ALERTS);
        labels.add(getString(R.string.reminder_cancel));
        actions.add(ReminderAction.CANCEL);

        new AlertDialog.Builder(this)
                .setTitle(target.existingReminder == null
                        ? R.string.reminder_add
                        : R.string.reminder_edit)
                .setItems(labels.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int action = actions.get(which);
                        if (action == ReminderAction.ONE_HOUR) {
                            saveReminder(target, ReminderTimeCalculator.oneHourAfter(System.currentTimeMillis()));
                        } else if (action == ReminderAction.TODAY_AT_SIX) {
                            saveReminder(target, ReminderTimeCalculator.todayAt(System.currentTimeMillis(), 18, 0));
                        } else if (action == ReminderAction.TOMORROW_AT_NINE) {
                            saveReminder(target, ReminderTimeCalculator.tomorrowAt(System.currentTimeMillis(), 9, 0));
                        } else if (action == ReminderAction.PRE_ALERTS) {
                            showPreAlertPicker(target);
                        } else if (action == ReminderAction.CUSTOM) {
                            showCustomReminderPicker(target);
                        } else {
                            cancelReminder(target);
                        }
                    }
                })
                .show();
    }

    private void showCustomReminderPicker(final ReminderTarget target) {
        final Calendar initial = Calendar.getInstance();
        if (target.existingReminder != null && target.existingReminder.getRemindAt() > System.currentTimeMillis()) {
            initial.setTimeInMillis(target.existingReminder.getRemindAt());
        } else {
            initial.add(Calendar.HOUR_OF_DAY, 1);
        }
        DatePickerDialog dateDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        final Calendar selected = Calendar.getInstance();
                        selected.set(year, month, dayOfMonth, initial.get(Calendar.HOUR_OF_DAY), initial.get(Calendar.MINUTE), 0);
                        selected.set(Calendar.MILLISECOND, 0);
                        new TimePickerDialog(
                                MainActivity.this,
                                new TimePickerDialog.OnTimeSetListener() {
                                    @Override
                                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                        selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                        selected.set(Calendar.MINUTE, minute);
                                        saveReminder(target, selected.getTimeInMillis());
                                    }
                                },
                                selected.get(Calendar.HOUR_OF_DAY),
                                selected.get(Calendar.MINUTE),
                                true)
                                .show();
                    }
                },
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH));
        dateDialog.show();
    }

    private void saveReminder(ReminderTarget target, long remindAt) {
        saveReminderInternal(
                target,
                remindAt,
                ReminderRecord.SOURCE_MANUAL,
                "",
                0L,
                false);
    }

    private void saveAutomaticReminder(
            final ReminderTarget target,
            NaturalLanguageReminderParser.ParseResult parsed) {
        if (parsed == null || !parsed.isAutoEligible() || parsed.getCandidate() == null) {
            return;
        }
        NaturalLanguageReminderParser.Candidate candidate = parsed.getCandidate();
        if (!saveReminderInternal(
                target,
                candidate.getTriggerAt(),
                ReminderRecord.SOURCE_NATURAL,
                parsed.getSourceExpression(),
                parsed.getReferenceAt(),
                true)) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.p1_natural_time_title)
                .setMessage(getString(R.string.p1_natural_time_auto_message, candidate.getDisplayTime()))
                .setNegativeButton(R.string.p1_natural_time_undo, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cancelReminder(target);
                    }
                })
                .setPositiveButton(R.string.p1_natural_time_modify, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showReminderPicker(target);
                    }
                })
                .show();
        requestReminderPermissions();
    }

    private boolean saveReminderInternal(
            ReminderTarget target,
            long remindAt,
            String source,
            String sourceExpression,
            long naturalReferenceAt,
            boolean automatic) {
        if (target == null || target.taskId.length() == 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (remindAt <= now) {
            Toast.makeText(this, R.string.reminder_overdue, Toast.LENGTH_LONG).show();
            return false;
        }
        ReminderRecord existing = database.getReminderByTaskId(target.taskId);
        int notificationId = existing == null
                ? ReminderIds.notificationIdForTaskId(target.taskId)
                : existing.getNotificationId();
        String remoteRemindAtText = existing == null
                ? target.remoteRemindAtText
                : existing.getRemoteRemindAtText();
        String expression = sourceExpression == null ? "" : sourceExpression;
        String signature = automatic
                ? target.taskId + "|" + source + "|" + expression + "|" + naturalReferenceAt
                : target.taskId + "|manual|" + TodoDateTime.format(remindAt);
        ReminderRecord record = new ReminderRecord(
                existing == null ? 0L : existing.getReminderId(),
                target.taskId,
                target.localNoteId,
                target.taskText,
                target.sourcePath,
                target.sourceBlockId,
                target.dueAt,
                remindAt,
                0L,
                ReminderRecord.STATUS_SCHEDULED,
                notificationId,
                now,
                target.dueAtText,
                TodoDateTime.format(remindAt),
                remoteRemindAtText,
                java.util.TimeZone.getDefault().getID(),
                automatic ? source : ReminderRecord.SOURCE_MANUAL,
                expression,
                signature,
                automatic ? naturalReferenceAt : 0L,
                false);
        if (existing != null) {
            reminderScheduler.cancel(existing);
        }
        database.upsertReminder(record);
        ReminderScheduler.ScheduleResult scheduleResult = reminderScheduler.schedule(record);
        reminderScheduler.rescheduleOccurrencesForTask(target.taskId);
        if (!automatic) {
            requestReminderPermissions();
        }
        if (!automatic && !scheduleResult.isScheduled()) {
            Toast.makeText(this, R.string.reminder_system_delay, Toast.LENGTH_LONG).show();
        } else if (!automatic && scheduleResult.isExact()) {
            Toast.makeText(this, getString(R.string.reminder_saved, TodoDateTime.format(remindAt)), Toast.LENGTH_LONG).show();
        } else if (!automatic) {
            Toast.makeText(this, R.string.reminder_system_delay, Toast.LENGTH_LONG).show();
        }
        refreshNotes();
        todoAdapter.notifyDataSetChanged();
        return true;
    }

    private void cancelReminder(ReminderTarget target) {
        ReminderRecord existing = database.getReminderByTaskId(target.taskId);
        if (existing == null) {
            return;
        }
        reminderScheduler.cancel(existing);
        for (ReminderOccurrence occurrence : database.getReminderOccurrencesForTask(target.taskId)) {
            reminderScheduler.cancel(occurrence);
        }
        database.cancelReminderOccurrences(target.taskId);
        boolean suppressAutomatic = ReminderRecord.SOURCE_NATURAL.equals(existing.getReminderSource())
                || ReminderRecord.SOURCE_DUE_DEFAULT.equals(existing.getReminderSource());
        database.upsertReminder(existing.withStatus(
                ReminderRecord.STATUS_CANCELLED,
                0L).withAutoSuppressed(existing.isAutoSuppressed() || suppressAutomatic)
                .withLastSyncedAt(System.currentTimeMillis()));
        Toast.makeText(this, R.string.reminder_cancelled, Toast.LENGTH_SHORT).show();
        refreshNotes();
        todoAdapter.notifyDataSetChanged();
    }

    private void showPreAlertPicker(final ReminderTarget target) {
        if (target == null || target.taskId.length() == 0) {
            return;
        }
        if (target.dueAt <= 0L) {
            Toast.makeText(this, R.string.p1_multi_reminder_need_due, Toast.LENGTH_SHORT).show();
            return;
        }
        target.existingReminder = database.getReminderByTaskId(target.taskId);
        if (target.existingReminder == null) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.p1_multi_reminder)
                    .setMessage(R.string.p1_multi_reminder_need_primary)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.reminder_add, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            showReminderPicker(target);
                        }
                    })
                    .show();
            return;
        }

        boolean[] checked = new boolean[]{false, false};
        for (ReminderOccurrence occurrence : database.getReminderOccurrencesForTask(target.taskId)) {
            boolean active = !ReminderRecord.STATUS_CANCELLED.equals(occurrence.getStatus());
            if (ReminderOccurrence.KIND_DAY_BEFORE.equals(occurrence.getKind())) {
                checked[0] = active;
            } else if (ReminderOccurrence.KIND_HOUR_BEFORE.equals(occurrence.getKind())) {
                checked[1] = active;
            }
        }
        final boolean[] selected = checked.clone();
        String[] labels = new String[]{
                getString(R.string.p1_day_before),
                getString(R.string.p1_hour_before)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.p1_multi_reminder)
                .setMultiChoiceItems(labels, checked, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        selected[which] = isChecked;
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        savePreAlerts(target, selected);
                    }
                })
                .show();
    }

    private void savePreAlerts(ReminderTarget target, boolean[] selected) {
        ReminderRecord parent = database.getReminderByTaskId(target.taskId);
        if (parent == null || parent.getDueAt() <= 0L) {
            Toast.makeText(this, R.string.p1_multi_reminder_need_primary, Toast.LENGTH_SHORT).show();
            return;
        }
        long now = System.currentTimeMillis();
        String timeZoneId = java.util.TimeZone.getDefault().getID();
        String[] kinds = new String[]{
                ReminderOccurrence.KIND_DAY_BEFORE,
                ReminderOccurrence.KIND_HOUR_BEFORE
        };
        boolean[] choices = selected == null ? new boolean[]{false, false} : selected;
        List<ReminderOccurrence> existingOccurrences = database.getReminderOccurrencesForTask(target.taskId);
        for (int index = 0; index < kinds.length; index++) {
            ReminderOccurrence existing = findOccurrence(existingOccurrences, kinds[index]);
            if (!choices[index]) {
                if (existing != null) {
                    reminderScheduler.cancel(existing);
                    database.upsertReminderOccurrence(existing.withStatus(
                            ReminderRecord.STATUS_CANCELLED, 0L).withLastSyncedAt(now));
                }
                continue;
            }
            long triggerAt = ReminderOccurrence.KIND_DAY_BEFORE.equals(kinds[index])
                    ? ReminderTimeCalculator.dayBeforeAt(parent.getDueAt(), parent.getDueAtText())
                    : ReminderTimeCalculator.hourBeforeAt(parent.getDueAt(), parent.getDueAtText());
            String status = triggerAt > now
                    ? ReminderRecord.STATUS_SCHEDULED
                    : ReminderRecord.STATUS_OVERDUE;
            int notificationId = existing == null
                    ? ReminderIds.notificationIdForOccurrence(target.taskId, kinds[index])
                    : existing.getNotificationId();
            ReminderOccurrence occurrence = new ReminderOccurrence(
                    existing == null ? 0L : existing.getOccurrenceId(),
                    target.taskId,
                    kinds[index],
                    triggerAt,
                    0L,
                    status,
                    notificationId,
                    now,
                    timeZoneId);
            if (existing != null) {
                reminderScheduler.cancel(existing);
            }
            database.upsertReminderOccurrence(occurrence);
        }
        reminderScheduler.rescheduleOccurrencesForTask(target.taskId);
        requestReminderPermissions();
        Toast.makeText(this, R.string.p1_multi_reminder_saved, Toast.LENGTH_SHORT).show();
        todoAdapter.notifyDataSetChanged();
    }

    private ReminderOccurrence findOccurrence(List<ReminderOccurrence> occurrences, String kind) {
        if (occurrences == null) {
            return null;
        }
        for (ReminderOccurrence occurrence : occurrences) {
            if (occurrence != null && kind.equals(occurrence.getKind())) {
                return occurrence;
            }
        }
        return null;
    }

    private void requestReminderPermissions() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            boolean explained = getPreferences(MODE_PRIVATE).getBoolean("reminder_notification_explained", false);
            if (!explained) {
                getPreferences(MODE_PRIVATE).edit().putBoolean("reminder_notification_explained", true).apply();
                new AlertDialog.Builder(this)
                        .setTitle(R.string.reminder_add)
                        .setMessage(R.string.reminder_notification_permission_explain)
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                promptExactAlarmPermissionIfNeeded();
                            }
                        })
                        .setPositiveButton(R.string.reminder_open_settings, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                exactAlarmPromptAfterNotification = true;
                                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
                            }
                        })
                        .show();
                return;
            }
        }
        promptExactAlarmPermissionIfNeeded();
    }

    private void promptExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 31 || ReminderScheduler.isExactAlarmAllowed(this)) {
            return;
        }
        boolean explained = getPreferences(MODE_PRIVATE).getBoolean("reminder_exact_explained", false);
        if (explained) {
            return;
        }
        getPreferences(MODE_PRIVATE).edit().putBoolean("reminder_exact_explained", true).apply();
        new AlertDialog.Builder(this)
                .setTitle(R.string.reminder_add)
                .setMessage(R.string.reminder_exact_permission_explain)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.reminder_open_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            startActivity(new Intent(
                                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:" + getPackageName())));
                        } catch (Exception ignored) {
                            // The fallback non-exact alarm remains active.
                        }
                    }
                })
                .show();
    }

    private void refreshNotes() {
        String keyword = currentPage == PAGE_HISTORY ? searchInput.getText().toString() : "";
        noteAdapter.setNotes(database.searchNotes(keyword));
        int pendingCount = database.getPendingNotes().size();
        if (pendingCount == 0) {
            syncStatus.setText(getString(R.string.synced_badge));
        } else {
            syncStatus.setText("待同步 " + pendingCount);
        }
    }

    private void switchPage(int page) {
        int previousPage = currentPage;
        currentPage = page;
        applyPageVisibility();
        applyTheme(currentTheme);
        refreshNotes();
        if (page == PAGE_HOME) {
            focusInput();
        } else {
            rootLayout.requestFocus();
        }
        if (page == PAGE_TODO && previousPage != PAGE_TODO) {
            syncTodoItems();
        }
    }

    private void applyPageVisibility() {
        boolean home = currentPage == PAGE_HOME;
        boolean history = currentPage == PAGE_HISTORY;
        boolean todo = currentPage == PAGE_TODO;
        searchPanel.setVisibility(history ? View.VISIBLE : View.GONE);
        noteInputPanel.setVisibility(home ? View.VISIBLE : View.GONE);
        saveActions.setVisibility(home ? View.VISIBLE : View.GONE);
        historyHeader.setVisibility(todo ? View.GONE : View.VISIBLE);
        noteList.setVisibility(todo ? View.GONE : View.VISIBLE);
        todoPage.setVisibility(todo ? View.VISIBLE : View.GONE);
        historyTitle.setText(history ? R.string.history_page_title : R.string.history_title);
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.image_picker_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private void pasteLatestClipboardText() {
        ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager == null || !manager.hasPrimaryClip()) {
            Toast.makeText(this, R.string.clipboard_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        ClipData clip = manager.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            Toast.makeText(this, R.string.clipboard_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence text = clip.getItemAt(0).coerceToText(this);
        if (text == null || text.toString().trim().length() == 0) {
            Toast.makeText(this, R.string.clipboard_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        insertIntoNoteInput(text.toString().trim());
        focusInput();
    }

    private void handleIncomingShare(Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if (!Intent.ACTION_SEND.equals(action) && !Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            return;
        }

        StringBuilder sharedText = new StringBuilder();
        appendSharedText(sharedText, intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT));
        appendSharedText(sharedText, intent.getCharSequenceExtra(Intent.EXTRA_TITLE));
        appendSharedText(sharedText, intent.getCharSequenceExtra(Intent.EXTRA_TEXT));

        ArrayList<Uri> imageUris = new ArrayList<>();
        collectStreamUris(intent, imageUris);
        collectClipData(intent, sharedText, imageUris);

        if (sharedText.length() == 0 && imageUris.isEmpty()) {
            clearShareIntent(intent);
            return;
        }

        switchPage(PAGE_HOME);
        if (sharedText.length() > 0) {
            insertIntoNoteInput(sharedText.toString());
        }
        for (Uri imageUri : imageUris) {
            keepSharedUriPermission(imageUri, intent);
            try {
                Uri pendingImageUri = copySharedImageToPendingFile(imageUri);
                insertIntoNoteInput("![待上传图片](" + pendingImageUri.toString() + ")");
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.share_image_import_failed, e.getMessage()), Toast.LENGTH_LONG).show();
            }
        }
        Toast.makeText(this, R.string.share_imported, Toast.LENGTH_SHORT).show();
        focusInput();
        clearShareIntent(intent);
    }

    private void appendSharedText(StringBuilder builder, CharSequence text) {
        if (text == null) {
            return;
        }
        String value = text.toString().trim();
        if (value.length() == 0 || builder.toString().contains(value)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(value);
    }

    private void collectStreamUris(Intent intent, ArrayList<Uri> imageUris) {
        if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
            ArrayList<Uri> streams = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (streams != null) {
                for (Uri uri : streams) {
                    addImageUriIfNeeded(intent, imageUris, uri);
                }
            }
            return;
        }

        Uri stream = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        addImageUriIfNeeded(intent, imageUris, stream);
    }

    private void collectClipData(Intent intent, StringBuilder sharedText, ArrayList<Uri> imageUris) {
        ClipData clipData = intent.getClipData();
        if (clipData == null) {
            return;
        }
        for (int i = 0; i < clipData.getItemCount(); i++) {
            ClipData.Item item = clipData.getItemAt(i);
            if (item == null) {
                continue;
            }
            Uri uri = item.getUri();
            if (uri != null && isImageShare(intent, uri)) {
                addImageUriIfNeeded(intent, imageUris, uri);
                continue;
            }
            CharSequence text = item.coerceToText(this);
            appendSharedText(sharedText, text);
        }
    }

    private void addImageUriIfNeeded(Intent intent, ArrayList<Uri> imageUris, Uri uri) {
        if (uri == null || !isImageShare(intent, uri)) {
            return;
        }
        String value = uri.toString();
        for (Uri existing : imageUris) {
            if (existing != null && value.equals(existing.toString())) {
                return;
            }
        }
        imageUris.add(uri);
    }

    private boolean isImageShare(Intent intent, Uri uri) {
        String mimeType = null;
        try {
            mimeType = getContentResolver().getType(uri);
        } catch (Exception ignored) {
        }
        if (mimeType != null && mimeType.startsWith("image/")) {
            return true;
        }
        String intentType = intent == null ? null : intent.getType();
        return intentType != null && intentType.startsWith("image/");
    }

    private void keepSharedUriPermission(Uri uri, Intent sourceIntent) {
        if (uri == null || sourceIntent == null) {
            return;
        }
        int flags = sourceIntent.getFlags();
        boolean canRead = (flags & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0;
        boolean canPersist = (flags & Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) != 0;
        if (!canRead || !canPersist) {
            return;
        }
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
        }
    }

    private void clearShareIntent(Intent intent) {
        intent.setAction(null);
        intent.removeExtra(Intent.EXTRA_SUBJECT);
        intent.removeExtra(Intent.EXTRA_TITLE);
        intent.removeExtra(Intent.EXTRA_TEXT);
        intent.removeExtra(Intent.EXTRA_STREAM);
        intent.setClipData(null);
    }

    private Uri copySharedImageToPendingFile(Uri sourceUri) throws Exception {
        String mimeType = mimeTypeFor(sourceUri);
        String extension = extensionFor(sourceUri, mimeType);
        byte[] bytes = readAllBytes(sourceUri);
        File pendingDir = new File(getFilesDir(), PENDING_IMAGE_DIR);
        if (!pendingDir.exists() && !pendingDir.mkdirs()) {
            throw new IllegalStateException("Cannot create pending image folder.");
        }
        File target = new File(pendingDir, buildImageFileName(extension));
        FileOutputStream output = new FileOutputStream(target);
        try {
            output.write(bytes);
        } finally {
            output.close();
        }
        return Uri.fromFile(target);
    }

    private void insertIntoNoteInput(String text) {
        int start = Math.max(0, noteInput.getSelectionStart());
        int end = Math.max(0, noteInput.getSelectionEnd());
        int min = Math.min(start, end);
        int max = Math.max(start, end);
        Editable editable = noteInput.getText();
        String prefix = min > 0 && editable.charAt(min - 1) != '\n' ? "\n" : "";
        editable.replace(min, max, prefix + text);
        noteInput.setSelection(min + prefix.length() + text.length());
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
        int saveActionText = Color.parseColor(theme.getSaveButtonTextColor());
        int todoActionText = Color.parseColor(theme.getTodoButtonTextColor());

        applyPageVisibility();
        pullRoot.setBackgroundColor(screen);
        rootLayout.setBackgroundColor(screen);
        applySafeAreaPadding();
        applyFontStyle(claudeFontEnabled);
        appTitle.setTextColor(primary);
        styleLogoTitle(appTitle, claudeFontEnabled);
        syncStatus.setTextColor(accent);
        historyTitle.setTextColor(secondary);
        searchIcon.setTextColor(secondary);

        stylePanel(searchPanel, input, border, 14);
        stylePanel(noteInputPanel, input, border, 16);
        styleButton(saveButton, saveAction, saveActionText, 14);
        styleButton(saveTodoButton, todoAction, todoActionText, 14);
        styleButton(recordButton, accentDark, Color.WHITE, 14);
        styleBottomNav(bottomNav, screen);
        styleNavItem(navHome, navHomeIcon, navHomeLabel, currentPage == PAGE_HOME ? accentDark : secondary, currentPage == PAGE_HOME);
        styleNavItem(navTags, navTagsIcon, navTagsLabel, currentPage == PAGE_HISTORY ? accentDark : secondary, currentPage == PAGE_HISTORY);
        styleNavItem(navStats, navStatsIcon, navStatsLabel, currentPage == PAGE_TODO ? accentDark : secondary, currentPage == PAGE_TODO);
        styleNavItem(navMine, navMineIcon, navMineLabel, secondary, false);
        styleIconButton(uploadImageButton, secondary);
        styleIconButton(clipboardButton, secondary);
        todoPageTitle.setTextColor(secondary);
        todoEmptyTitle.setTextColor(primary);
        todoEmptyMessage.setTextColor(secondary);
        pullIndicatorIcon.setColorFilter(accentDark);
        pullIndicatorText.setTextColor(accentDark);
        updatePullPrompt();
        searchInput.setTextColor(primary);
        searchInput.setHintTextColor(secondary);
        noteInput.setTextColor(primary);
        noteInput.setHintTextColor(withAlpha(secondary, 82));
        saveButton.setText(getString(R.string.save_note));
        saveTodoButton.setText(getString(R.string.save_todo));
        setStartIcon(recordButton, R.drawable.ic_top_sync, Color.WHITE, 18);
        setStartIcon(saveButton, R.drawable.ic_action_save, saveActionText, 18);
        setStartIcon(saveTodoButton, R.drawable.ic_action_todo, todoActionText, 18);
        setStartIcon(syncStatus, R.drawable.ic_status_synced, accent, 15);
        setElevationDp(searchPanel, 0);
        setElevationDp(noteInputPanel, 0);
        setElevationDp(saveButton, 0);
        setElevationDp(saveTodoButton, 0);
        setElevationDp(recordButton, 0);
        setElevationDp(bottomNav, 0);

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(screen);
            getWindow().setNavigationBarColor(screen);
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                int flags = "ink".equals(theme.getKey()) ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                getWindow().getDecorView().setSystemUiVisibility(flags);
            }
        }

        noteAdapter.setTheme(theme, claudeFontEnabled);
        todoAdapter.setTheme(theme, claudeFontEnabled);
    }

    private void bindPullGesture() {
        View.OnTouchListener listener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                return handlePullTouch(view, event);
            }
        };
        noteInputPanel.setOnTouchListener(listener);
        noteInput.setOnTouchListener(listener);
    }

    private boolean handlePullTouch(View source, MotionEvent event) {
        if (!isPullSourceAllowed(source)) {
            return false;
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            pullStartY = event.getRawY();
            isPulling = false;
            return false;
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float distance = event.getRawY() - pullStartY;
            if (distance <= dp(8) && !isPulling) {
                return false;
            }
            if (distance > 0) {
                isPulling = true;
                updatePullPrompt();
                float translation = Math.min(distance * 0.55f, dp(PULL_MAX_DP));
                rootLayout.setTranslationY(translation);
                pullIndicator.setAlpha(Math.min(1f, translation / dp(72)));
                return true;
            }
        }
        if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            if (!isPulling) {
                return false;
            }
            float translation = rootLayout.getTranslationY();
            boolean triggered = translation >= dp(PULL_TRIGGER_DP);
            resetPullGesture();
            if (triggered) {
                vibrateOnce();
                triggerPullAction();
            }
            return true;
        }
        return false;
    }

    private boolean isPullSourceAllowed(View source) {
        return currentPage == PAGE_HOME && (source == noteInputPanel || source == noteInput);
    }

    private void resetPullGesture() {
        isPulling = false;
        rootLayout.animate().translationY(0f).setDuration(160).start();
        pullIndicator.animate().alpha(0f).setDuration(120).start();
    }

    private void triggerPullAction() {
        int action = PullGestureAction.decide(noteInput.getText().toString());
        if (action == PullGestureAction.SAVE_NOTE) {
            saveCurrentNote();
        } else if (action == PullGestureAction.SAVE_TODO) {
            saveCurrentTodo();
        } else {
            syncPendingNotes();
        }
    }

    private void updatePullPrompt() {
        if (pullIndicatorText == null || noteInput == null) {
            return;
        }
        int action = PullGestureAction.decide(noteInput.getText().toString());
        int promptRes = action == PullGestureAction.SAVE_NOTE || action == PullGestureAction.SAVE_TODO
                ? R.string.pull_prompt_save
                : R.string.pull_prompt_sync;
        pullIndicatorText.setText(promptRes);
    }

    private void vibrateOnce() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null) {
            return;
        }
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(35);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) {
                return;
            }
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {
            }
            insertIntoNoteInput("![待上传图片](" + uri.toString() + ")");
            Toast.makeText(this, R.string.image_inserted, Toast.LENGTH_SHORT).show();
            focusInput();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_POST_NOTIFICATIONS) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.reminder_system_delay, Toast.LENGTH_LONG).show();
            }
            if (exactAlarmPromptAfterNotification) {
                exactAlarmPromptAfterNotification = false;
                promptExactAlarmPermissionIfNeeded();
            }
            return;
        }
        if (requestCode != REQUEST_READ_IMAGES_PERMISSION) {
            return;
        }
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        boolean shouldBatchSync = pendingBatchSyncAfterPermission;
        FlashNote singleNote = pendingSingleSyncAfterPermission;
        pendingBatchSyncAfterPermission = false;
        pendingSingleSyncAfterPermission = null;

        if (!granted) {
            Toast.makeText(this, R.string.image_read_permission_denied, Toast.LENGTH_LONG).show();
            return;
        }
        if (singleNote != null) {
            resyncSingleNote(singleNote);
        } else if (shouldBatchSync) {
            syncPendingNotes();
        }
    }

    private byte[] readAllBytes(Uri uri) throws Exception {
        InputStream input = getContentResolver().openInputStream(uri);
        if (input == null) {
            throw new IllegalStateException("Cannot open image.");
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } finally {
            input.close();
        }
    }

    private boolean requestImageReadPermissionIfNeeded(List<FlashNote> notes) {
        if (!hasMediaStorePendingImage(notes)) {
            return false;
        }
        String permission = imageReadPermission();
        if (permission == null || checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        requestPermissions(new String[]{permission}, REQUEST_READ_IMAGES_PERMISSION);
        return true;
    }

    private String imageReadPermission() {
        if (Build.VERSION.SDK_INT < 23) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= 33) {
            return Manifest.permission.READ_MEDIA_IMAGES;
        }
        return Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private boolean hasMediaStorePendingImage(List<FlashNote> notes) {
        if (notes == null) {
            return false;
        }
        for (FlashNote note : notes) {
            if (note == null) {
                continue;
            }
            Matcher matcher = PENDING_IMAGE_PATTERN.matcher(note.getContent() == null ? "" : note.getContent());
            while (matcher.find()) {
                Uri uri = Uri.parse(matcher.group(1));
                if ("content".equals(uri.getScheme())) {
                    String authority = uri.getAuthority();
                    if (authority != null && authority.toLowerCase(Locale.US).contains("media")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String buildImageFileName(String extension) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
        String suffix = Long.toHexString(System.currentTimeMillis()).toLowerCase(Locale.US);
        if (suffix.length() > 6) {
            suffix = suffix.substring(suffix.length() - 6);
        }
        return "flash-" + format.format(new Date()) + "-" + suffix + "." + extension;
    }

    private String extensionFor(Uri uri, String mimeType) {
        if (mimeType != null) {
            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (extension != null && extension.trim().length() > 0) {
                return extension.toLowerCase(Locale.US);
            }
        }
        String path = uri == null ? "" : uri.getLastPathSegment();
        int dot = path == null ? -1 : path.lastIndexOf('.');
        if (dot >= 0 && dot < path.length() - 1) {
            String extension = path.substring(dot + 1).toLowerCase(Locale.US);
            if (extension.matches("[a-z0-9]{2,5}")) {
                return extension;
            }
        }
        return "jpg";
    }

    private String mimeTypeFor(Uri uri) {
        String mimeType = null;
        try {
            mimeType = getContentResolver().getType(uri);
        } catch (Exception ignored) {
        }
        if (mimeType != null && mimeType.trim().length() > 0) {
            return mimeType;
        }
        String extension = extensionFor(uri, null);
        String guessed = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        return guessed == null ? "application/octet-stream" : guessed;
    }

    private void deletePendingLocalImage(Uri uri) {
        if (uri == null || !"file".equals(uri.getScheme())) {
            return;
        }
        try {
            File file = new File(uri.getPath());
            File pendingDir = new File(getFilesDir(), PENDING_IMAGE_DIR).getCanonicalFile();
            File canonicalFile = file.getCanonicalFile();
            if (canonicalFile.getPath().startsWith(pendingDir.getPath()) && canonicalFile.isFile()) {
                canonicalFile.delete();
            }
        } catch (Exception ignored) {
        }
    }

    private void syncPendingNotes() {
        final SyncSettings settings = SyncSettings.load(this);
        final List<FlashNote> pending = database.getPendingNotes();
        if (pending.isEmpty()) {
            Toast.makeText(this, R.string.sync_no_pending, Toast.LENGTH_SHORT).show();
            return;
        }
        if (requestImageReadPermissionIfNeeded(pending)) {
            pendingBatchSyncAfterPermission = true;
            pendingSingleSyncAfterPermission = null;
            return;
        }
        Toast.makeText(this, R.string.sync_queued, Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final PreparedNotes prepared = prepareImagesForSync(pending, settings);
                final WebDavMarkdownSync.Result result = prepared.isReady()
                        ? new WebDavMarkdownSync().sync(prepared.getNotes(), settings, database)
                        : WebDavMarkdownSync.Result.failed(prepared.getErrorMessage());
                if (result.isSynced()) {
                    database.markSyncState(prepared.getNotes(), FlashNoteDatabase.SYNC_SYNCED);
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

    private void stylePanel(View panel, int backgroundColor, int borderColor, int radiusDp) {
        panel.setBackground(makeRoundedBackground(backgroundColor, borderColor, radiusDp));
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private void styleButton(Button button, int backgroundColor, int textColor, int radiusDp) {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            button.setBackgroundTintList(null);
            button.setStateListAnimator(null);
            button.setElevation(0f);
            button.setTranslationZ(0f);
        }
        button.setBackground(makeRoundedBackground(backgroundColor, Color.TRANSPARENT, radiusDp));
        button.setTextColor(textColor);
        button.setAllCaps(false);
        button.setIncludeFontPadding(false);
        button.setCompoundDrawablePadding((int) dp(7));
    }

    private void styleLogoTitle(TextView title, boolean useClaudeFont) {
        String family = useClaudeFont ? "serif" : "sans-serif-medium";
        title.setTypeface(android.graphics.Typeface.create(family, android.graphics.Typeface.BOLD));
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            title.setLetterSpacing(0.08f);
        }
        title.setIncludeFontPadding(false);
    }

    private void applyFontStyle(boolean useClaudeFont) {
        Typeface body = Typeface.create(useClaudeFont ? "serif" : "sans-serif", Typeface.NORMAL);
        Typeface medium = Typeface.create(useClaudeFont ? "serif" : "sans-serif-medium", Typeface.NORMAL);
        applyTypeface(appTitle, medium, Typeface.BOLD);
        applyTypeface(syncStatus, medium, Typeface.BOLD);
        applyTypeface(historyTitle, medium, Typeface.BOLD);
        applyTypeface(searchInput, body, Typeface.NORMAL);
        applyTypeface(noteInput, body, Typeface.NORMAL);
        applyTypeface(saveButton, medium, Typeface.BOLD);
        applyTypeface(saveTodoButton, medium, Typeface.BOLD);
        applyTypeface(recordButton, medium, Typeface.BOLD);
        applyTypeface(todoPageTitle, medium, Typeface.BOLD);
        applyTypeface(todoEmptyTitle, medium, Typeface.BOLD);
        applyTypeface(todoEmptyMessage, body, Typeface.NORMAL);
        applyTypeface(pullIndicatorText, medium, Typeface.BOLD);
        applyTypeface(navHomeLabel, medium, Typeface.BOLD);
        applyTypeface(navTagsLabel, body, Typeface.NORMAL);
        applyTypeface(navStatsLabel, body, Typeface.NORMAL);
        applyTypeface(navMineLabel, body, Typeface.NORMAL);
    }

    private void applyTypeface(TextView view, Typeface typeface, int style) {
        if (view != null) {
            view.setTypeface(typeface, style);
        }
    }

    private void applySafeAreaPadding() {
        int topPadding = getStatusBarHeight() + Math.round(dp(18));
        rootLayout.setPadding(Math.round(dp(22)), topPadding, Math.round(dp(22)), Math.round(dp(10)));
    }

    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return Math.round(dp(24));
    }

    private void styleIconButton(ImageButton button, int iconColor) {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            button.setBackgroundTintList(null);
            button.setStateListAnimator(null);
            button.setElevation(0f);
            button.setTranslationZ(0f);
        }
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setColorFilter(iconColor);
    }

    private void styleBottomNav(View nav, int backgroundColor) {
        nav.setBackground(makeRoundedBackground(backgroundColor, Color.TRANSPARENT, 22));
    }

    private void styleNavItem(View item, ImageView icon, TextView label, int color, boolean active) {
        item.setPadding(0, (int) dp(8), 0, (int) dp(9));
        icon.setColorFilter(color);
        label.setTextColor(color);
        Typeface typeface = Typeface.create(claudeFontEnabled ? "serif" : "sans-serif", Typeface.NORMAL);
        label.setTypeface(typeface, active ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void setStartIcon(TextView view, int drawableRes, int color, int sizeDp) {
        Drawable icon = getResources().getDrawable(drawableRes).mutate();
        icon.setBounds(0, 0, Math.round(dp(sizeDp)), Math.round(dp(sizeDp)));
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            icon.setTint(color);
        }
        view.setCompoundDrawables(icon, null, null, null);
        view.setCompoundDrawablePadding((int) dp(6));
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
            view.setTranslationZ(0f);
            view.setStateListAnimator(null);
        }
    }

    private void showNoteActions(final FlashNote note) {
        final String toggleLabel = note.getNoteType() == FlashNote.TYPE_TODO
                ? getString(R.string.convert_to_note)
                : getString(R.string.convert_to_todo);
        final boolean isTodo = note.getNoteType() == FlashNote.TYPE_TODO;
        ArrayList<String> actionList = new ArrayList<>();
        actionList.add(getString(R.string.edit_note));
        actionList.add(getString(R.string.resync_note));
        if (isTodo) {
            actionList.add(getString(R.string.reminder_edit));
        }
        actionList.add(toggleLabel);
        actionList.add(getString(R.string.delete_note));
        new AlertDialog.Builder(this)
                .setTitle(R.string.note_actions_title)
                .setItems(actionList.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            showEditNoteDialog(note);
                        } else if (which == 1) {
                            resyncSingleNote(note);
                        } else if (isTodo && which == 2) {
                            showReminderPicker(ReminderTarget.forLocalNote(note));
                        } else if (which == (isTodo ? 3 : 2)) {
                            toggleNoteType(note);
                        } else if (which == (isTodo ? 4 : 3)) {
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
        if (nextType != FlashNote.TYPE_TODO) {
            cancelReminder(ReminderTarget.forLocalNote(note));
        }
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
                        if (note.getNoteType() == FlashNote.TYPE_TODO) {
                            cancelReminder(ReminderTarget.forLocalNote(note));
                        }
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
        if (requestImageReadPermissionIfNeeded(Collections.singletonList(note))) {
            pendingBatchSyncAfterPermission = false;
            pendingSingleSyncAfterPermission = note;
            return;
        }
        Toast.makeText(this, R.string.sync_queued, Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final PreparedNotes prepared = prepareImagesForSync(Collections.singletonList(note), settings);
                final WebDavMarkdownSync.Result result = prepared.isReady()
                        ? new WebDavMarkdownSync().sync(prepared.getNotes(), settings, database)
                        : WebDavMarkdownSync.Result.failed(prepared.getErrorMessage());
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

    private PreparedNotes prepareImagesForSync(List<FlashNote> notes, SyncSettings settings) {
        if (notes == null || notes.isEmpty()) {
            return PreparedNotes.ready(new ArrayList<FlashNote>());
        }
        if (!settings.isReady()) {
            return PreparedNotes.ready(notes);
        }
        ArrayList<FlashNote> prepared = new ArrayList<>();
        for (FlashNote note : notes) {
            ImageContentResult contentResult = uploadPendingImages(note.getContent(), settings);
            if (!contentResult.isReady()) {
                return PreparedNotes.failed(contentResult.getErrorMessage());
            }
            String nextContent = contentResult.getContent();
            if (!nextContent.equals(note.getContent())) {
                database.updateNoteContent(note.getId(), nextContent);
            }
            prepared.add(new FlashNote(
                    note.getId(),
                    nextContent,
                    note.getCreatedAtMillis(),
                    note.getSyncState(),
                    note.getNoteType()));
        }
        return PreparedNotes.ready(prepared);
    }

    private ImageContentResult uploadPendingImages(String content, SyncSettings settings) {
        String safeContent = content == null ? "" : content;
        Matcher matcher = PENDING_IMAGE_PATTERN.matcher(safeContent);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            Uri uri = Uri.parse(matcher.group(1));
            try {
                String mimeType = mimeTypeFor(uri);
                String extension = extensionFor(uri, mimeType);
                String fileName = buildImageFileName(extension);
                byte[] bytes = readAllBytes(uri);
                String remotePath = ObsidianImageAsset.buildAssetRemotePath(settings.getRemotePath(), fileName);
                WebDavImageUploader.Result result = new WebDavImageUploader().upload(
                        bytes,
                        remotePath,
                        mimeType == null ? "application/octet-stream" : mimeType,
                        settings);
                if (!result.isUploaded()) {
                    return ImageContentResult.failed(result.getMessage());
                }
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(ObsidianImageAsset.buildObsidianEmbed(fileName)));
                deletePendingLocalImage(uri);
            } catch (Exception e) {
                return ImageContentResult.failed(e.getMessage());
            }
        }
        matcher.appendTail(buffer);
        return ImageContentResult.ready(buffer.toString());
    }

    private static final class PreparedNotes {
        private final List<FlashNote> notes;
        private final String errorMessage;

        private PreparedNotes(List<FlashNote> notes, String errorMessage) {
            this.notes = notes;
            this.errorMessage = errorMessage;
        }

        static PreparedNotes ready(List<FlashNote> notes) {
            return new PreparedNotes(notes, null);
        }

        static PreparedNotes failed(String errorMessage) {
            return new PreparedNotes(Collections.<FlashNote>emptyList(), errorMessage);
        }

        boolean isReady() {
            return errorMessage == null;
        }

        List<FlashNote> getNotes() {
            return notes;
        }

        String getErrorMessage() {
            return errorMessage;
        }
    }

    private static final class ReminderAction {
        static final int ONE_HOUR = 1;
        static final int TODAY_AT_SIX = 2;
        static final int TOMORROW_AT_NINE = 3;
        static final int CUSTOM = 4;
        static final int CANCEL = 5;
        static final int PRE_ALERTS = 6;

        private ReminderAction() {
        }
    }

    private static final class ReminderTarget {
        private final String taskId;
        private final long localNoteId;
        private final String taskText;
        private final String sourcePath;
        private final String sourceBlockId;
        private final long dueAt;
        private final String dueAtText;
        private final String remoteRemindAtText;
        private ReminderRecord existingReminder;

        private ReminderTarget(
                String taskId,
                long localNoteId,
                String taskText,
                String sourcePath,
                String sourceBlockId,
                long dueAt,
                String dueAtText,
                String remoteRemindAtText) {
            this.taskId = taskId == null ? "" : taskId;
            this.localNoteId = localNoteId;
            this.taskText = taskText == null ? "" : taskText;
            this.sourcePath = sourcePath == null ? "" : sourcePath;
            this.sourceBlockId = sourceBlockId == null ? "" : sourceBlockId;
            this.dueAt = dueAt;
            this.dueAtText = dueAtText == null ? "" : dueAtText;
            this.remoteRemindAtText = remoteRemindAtText == null ? "" : remoteRemindAtText;
        }

        static ReminderTarget forLocalNote(FlashNote note) {
            return new ReminderTarget(
                    ReminderIds.localTaskId(note.getId()),
                    note.getId(),
                    note.getContent(),
                    "手机端待办",
                    "",
                    0L,
                    "",
                    "");
        }

        static ReminderTarget forRemoteTodo(TodoSyncItem item) {
            String taskId = item.getTaskId();
            if (taskId == null || taskId.length() == 0) {
                taskId = ReminderIds.generatedTaskId(item.getSourcePath(), item.getLineNumber(), item.getText());
            }
            return new ReminderTarget(
                    taskId,
                    0L,
                    item.getText(),
                    item.getSourcePath(),
                    item.getBlockId(),
                    TodoDateTime.parseDue(item.getDueAtText()),
                    item.getDueAtText(),
                    item.getRemindAtText());
        }
    }

    private static final class ImageContentResult {
        private final String content;
        private final String errorMessage;

        private ImageContentResult(String content, String errorMessage) {
            this.content = content;
            this.errorMessage = errorMessage;
        }

        static ImageContentResult ready(String content) {
            return new ImageContentResult(content, null);
        }

        static ImageContentResult failed(String errorMessage) {
            return new ImageContentResult(null, errorMessage);
        }

        boolean isReady() {
            return errorMessage == null;
        }

        String getContent() {
            return content;
        }

        String getErrorMessage() {
            return errorMessage;
        }
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
        private boolean useClaudeFont;

        NoteAdapter(Context context) {
            inflater = LayoutInflater.from(context);
            dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        }

        void setNotes(List<FlashNote> nextNotes) {
            notes.clear();
            notes.addAll(nextNotes);
            notifyDataSetChanged();
        }

        void setTheme(ThemePalette nextTheme, boolean nextUseClaudeFont) {
            theme = nextTheme;
            useClaudeFont = nextUseClaudeFont;
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
                holder.reminderBadge = row.findViewById(R.id.reminderBadge);
                holder.deleteButton = row.findViewById(R.id.deleteButton);
                row.setTag(holder);
            } else {
                holder = (ViewHolder) row.getTag();
            }

            FlashNote note = notes.get(position);
            Typeface body = Typeface.create(useClaudeFont ? "serif" : "sans-serif", Typeface.NORMAL);
            Typeface medium = Typeface.create(useClaudeFont ? "serif" : "sans-serif-medium", Typeface.NORMAL);
            holder.content.setTypeface(body, Typeface.NORMAL);
            holder.time.setTypeface(body, Typeface.NORMAL);
            holder.syncBadge.setTypeface(medium, Typeface.BOLD);
            holder.todoBadge.setTypeface(medium, Typeface.BOLD);
            holder.content.setTextColor(Color.parseColor(theme.getPrimaryTextColor()));
            holder.time.setTextColor(Color.parseColor(theme.getSecondaryTextColor()));
            holder.noteCard.setBackground(makeRoundedBackground(
                    Color.parseColor(theme.getSurfaceColor()),
                    Color.parseColor(theme.getBorderColor()),
                    "paper".equals(theme.getKey()) ? 12 : 8));
            holder.timelineLine.setBackgroundColor(Color.parseColor("paper".equals(theme.getKey()) ? "#D6DDD7" : theme.getBorderColor()));
            holder.timelineDot.setBackground(makeRoundedBackground(Color.parseColor(theme.getAccentDarkColor()), Color.TRANSPARENT, 8));
            setElevationDp(holder.noteCard, 0);
            holder.content.setText(note.getContent());
            holder.time.setText(dateTimeFormat.format(new Date(note.getCreatedAtMillis())));
            holder.syncBadge.setVisibility(note.getSyncState() == FlashNoteDatabase.SYNC_SYNCED ? View.VISIBLE : View.GONE);
            holder.syncBadge.setTextColor(Color.parseColor(theme.getAccentDarkColor()));
            holder.todoBadge.setVisibility(note.getNoteType() == FlashNote.TYPE_TODO ? View.VISIBLE : View.GONE);
            ReminderRecord localReminder = note.getNoteType() == FlashNote.TYPE_TODO
                    ? database.getReminderForLocalNote(note.getId())
                    : null;
            if (localReminder != null
                    && (ReminderRecord.STATUS_SCHEDULED.equals(localReminder.getStatus())
                    || ReminderRecord.STATUS_SNOOZED.equals(localReminder.getStatus()))) {
                String remindText = localReminder.getRemindAtText().length() > 0
                        ? localReminder.getRemindAtText()
                        : TodoDateTime.format(localReminder.getRemindAt());
                holder.reminderBadge.setText("🔔 " + remindText);
                holder.reminderBadge.setVisibility(View.VISIBLE);
            } else if (localReminder != null && ReminderRecord.STATUS_OVERDUE.equals(localReminder.getStatus())) {
                holder.reminderBadge.setText("🔔 已过期");
                holder.reminderBadge.setVisibility(View.VISIBLE);
            } else {
                holder.reminderBadge.setVisibility(View.GONE);
            }
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                holder.deleteButton.setBackgroundTintList(null);
                holder.deleteButton.setStateListAnimator(null);
                holder.deleteButton.setElevation(0f);
                holder.deleteButton.setTranslationZ(0f);
            }
            holder.deleteButton.setBackgroundColor(Color.TRANSPARENT);
            holder.deleteButton.setImageResource(R.drawable.ic_timeline_delete);
            holder.deleteButton.setColorFilter(Color.parseColor(theme.getSecondaryTextColor()));
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

    private final class TodoAdapter extends BaseAdapter {
        private final List<TodoRow> rows = new ArrayList<>();
        private ThemePalette theme = ThemePalette.findByKey("paper");
        private boolean useClaudeFont;

        TodoAdapter(Context context) {
        }

        void setItems(List<TodoSyncItem> nextItems) {
            rows.clear();
            String currentSourcePath = null;
            for (TodoSyncItem item : nextItems) {
                String sourcePath = item.getSourcePath();
                if (!sourcePath.equals(currentSourcePath)) {
                    currentSourcePath = sourcePath;
                    rows.add(TodoRow.header(sourcePath));
                }
                rows.add(TodoRow.item(item));
            }
            notifyDataSetChanged();
        }

        int findPositionByTaskId(String taskId) {
            if (taskId == null || taskId.length() == 0) {
                return -1;
            }
            for (int index = 0; index < rows.size(); index++) {
                TodoRow row = rows.get(index);
                if (!row.isHeader() && taskId.equals(row.item.getTaskId())) {
                    return index;
                }
            }
            return -1;
        }

        void setTheme(ThemePalette nextTheme, boolean nextUseClaudeFont) {
            theme = nextTheme;
            useClaudeFont = nextUseClaudeFont;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return rows.size();
        }

        @Override
        public Object getItem(int position) {
            return rows.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TodoRow todoRow = rows.get(position);
            if (todoRow.isHeader()) {
                return createTodoHeaderView(todoRow);
            }
            return createTodoItemView(todoRow.item);
        }

        private View createTodoHeaderView(TodoRow rowData) {
            LinearLayout row = new LinearLayout(MainActivity.this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding((int) dp(4), (int) dp(14), (int) dp(4), (int) dp(6));
            row.setLayoutParams(new ListView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            Typeface body = Typeface.create(useClaudeFont ? "serif" : "sans-serif", Typeface.NORMAL);
            Typeface medium = Typeface.create(useClaudeFont ? "serif" : "sans-serif-medium", Typeface.NORMAL);

            TextView title = new TextView(MainActivity.this);
            title.setText(displaySourceTitle(rowData.sourcePath));
            title.setTextColor(Color.parseColor(theme.getPrimaryTextColor()));
            title.setTextSize(15);
            title.setTypeface(medium, Typeface.BOLD);
            title.setIncludeFontPadding(false);
            row.addView(title, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            String folder = displaySourceFolder(rowData.sourcePath);
            if (folder.length() > 0) {
                TextView meta = new TextView(MainActivity.this);
                meta.setText(folder);
                meta.setTextColor(Color.parseColor(theme.getSecondaryTextColor()));
                meta.setTextSize(11);
                meta.setTypeface(body, Typeface.NORMAL);
                LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                metaParams.topMargin = (int) dp(4);
                row.addView(meta, metaParams);
            }
            return row;
        }

        private View createTodoItemView(TodoSyncItem item) {
            LinearLayout outer = new LinearLayout(MainActivity.this);
            outer.setOrientation(LinearLayout.VERTICAL);
            outer.setPadding(0, (int) dp(3), 0, (int) dp(5));
            outer.setLayoutParams(new ListView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout card = new LinearLayout(MainActivity.this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding((int) dp(14), (int) dp(11), (int) dp(14), (int) dp(11));
            card.setBackground(makeRoundedBackground(
                    Color.parseColor(theme.getSurfaceColor()),
                    Color.parseColor(theme.getBorderColor()),
                    "paper".equals(theme.getKey()) ? 12 : 8));
            setElevationDp(card, 0);
            outer.addView(card, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            Typeface body = Typeface.create(useClaudeFont ? "serif" : "sans-serif", Typeface.NORMAL);
            TextView content = new TextView(MainActivity.this);
            content.setText(cleanTodoText(item.getText()));
            content.setTextColor(Color.parseColor(theme.getPrimaryTextColor()));
            content.setTextSize(15);
            content.setTypeface(body, Typeface.NORMAL);
            content.setLineSpacing(dp(2), 1f);
            card.addView(content, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView meta = new TextView(MainActivity.this);
            meta.setText(buildTodoMeta(item));
            meta.setTextColor(Color.parseColor(theme.getSecondaryTextColor()));
            meta.setTextSize(11);
            meta.setTypeface(body, Typeface.NORMAL);
            LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            metaParams.topMargin = (int) dp(7);
            card.addView(meta, metaParams);
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showReminderPicker(ReminderTarget.forRemoteTodo(item));
                }
            });
            return outer;
        }

        private String buildTodoMeta(TodoSyncItem item) {
            StringBuilder builder = new StringBuilder();
            if (item.getLineNumber() > 0) {
                builder.append("第 ").append(item.getLineNumber()).append(" 行");
            }
            if (item.getNote().length() > 0) {
                if (builder.length() > 0) {
                    builder.append("\n");
                }
                builder.append("备注：").append(item.getNote());
            }
            if (autoConflictTaskIds.contains(item.getTaskId())) {
                if (builder.length() > 0) {
                    builder.append("\n");
                }
                builder.append("⚠ ").append(getString(R.string.p1_natural_time_conflict));
            }
            ReminderRecord reminder = database.getReminderByTaskId(item.getTaskId());
            if (builder.length() > 0) {
                builder.append("\n");
            }
            if (reminder != null && ReminderRecord.STATUS_OVERDUE.equals(reminder.getStatus())) {
                builder.append("🔔 已过期");
            } else if (reminder != null
                    && (ReminderRecord.STATUS_SCHEDULED.equals(reminder.getStatus())
                    || ReminderRecord.STATUS_SNOOZED.equals(reminder.getStatus()))) {
                String remindText = reminder.getRemindAtText().length() > 0
                        ? reminder.getRemindAtText()
                        : TodoDateTime.format(reminder.getRemindAt());
                if (ReminderRecord.STATUS_SNOOZED.equals(reminder.getStatus())) {
                    remindText = "稍后 " + TodoDateTime.format(reminder.getSnoozeUntil());
                }
                builder.append("🔔 ").append(remindText);
            } else if (item.getRemindAtText().length() > 0) {
                builder.append("🔔 ").append(item.getRemindAtText());
            } else {
                builder.append("○ 未设置提醒");
            }
            return builder.length() == 0 ? "来自 Obsidian 待办同步" : builder.toString();
        }

        private String cleanTodoText(String text) {
            String cleaned = text == null ? "" : text.trim();
            if (cleaned.startsWith("待办 ")) {
                cleaned = cleaned.substring(3).trim();
            }
            return cleaned;
        }

        private String displaySourceTitle(String sourcePath) {
            String path = sourcePath == null ? "" : sourcePath.replace('\\', '/');
            int slash = path.lastIndexOf('/');
            String name = slash >= 0 ? path.substring(slash + 1) : path;
            return name.replaceAll("\\.md$", "");
        }

        private String displaySourceFolder(String sourcePath) {
            String path = sourcePath == null ? "" : sourcePath.replace('\\', '/');
            int slash = path.lastIndexOf('/');
            return slash > 0 ? path.substring(0, slash) : "";
        }
    }

    private static final class TodoRow {
        private final String sourcePath;
        private final TodoSyncItem item;

        private TodoRow(String sourcePath, TodoSyncItem item) {
            this.sourcePath = sourcePath == null ? "" : sourcePath;
            this.item = item;
        }

        static TodoRow header(String sourcePath) {
            return new TodoRow(sourcePath, null);
        }

        static TodoRow item(TodoSyncItem item) {
            return new TodoRow(item == null ? "" : item.getSourcePath(), item);
        }

        boolean isHeader() {
            return item == null;
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
        TextView reminderBadge;
        ImageButton deleteButton;
    }

}

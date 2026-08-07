package com.dabawei.flashnote;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.widget.RemoteViews;

import java.util.List;

public final class FlashNoteWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateAllWidgets(context, appWidgetManager, appWidgetIds);
    }

    public static void refresh(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName widget = new ComponentName(context, FlashNoteWidgetProvider.class);
        updateAllWidgets(context, manager, manager.getAppWidgetIds(widget));
    }

    private static void updateAllWidgets(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            manager.updateAppWidget(appWidgetId, buildViews(context));
        }
    }

    private static RemoteViews buildViews(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.flash_note_widget);
        SharedPreferences prefs = context.getSharedPreferences("dabawei_flashnote_prefs", Context.MODE_PRIVATE);
        String stored = prefs.getString("theme_key", "system");
        String migrated = ThemePalette.migratePreference(stored);
        if (!migrated.equals(stored)) {
            prefs.edit().putString("theme_key", migrated).apply();
        }
        boolean systemIsDark = (context.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        ThemePalette theme = ThemePalette.resolve(migrated, systemIsDark);
        boolean dark = "dark".equals(theme.getKey());
        views.setInt(R.id.widgetRoot, "setBackgroundResource",
                dark ? R.drawable.widget_background_dark : R.drawable.widget_background);
        views.setInt(R.id.widgetAction, "setBackgroundResource",
                dark ? R.drawable.widget_button_background_dark : R.drawable.widget_button_background);
        views.setTextColor(R.id.widgetTitle, Color.parseColor(theme.getPrimaryTextColor()));
        views.setTextColor(R.id.widgetRecent, Color.parseColor(theme.getSecondaryTextColor()));
        views.setTextColor(R.id.widgetAction, Color.parseColor(theme.getPrimaryButtonTextColor()));
        views.setTextViewText(R.id.widgetRecent, latestNoteText(context));

        Intent intent = new Intent(context, QuickCaptureActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                1001,
                intent,
                pendingIntentFlags());
        views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent);
        views.setOnClickPendingIntent(R.id.widgetAction, pendingIntent);
        return views;
    }

    private static String latestNoteText(Context context) {
        FlashNoteDatabase database = new FlashNoteDatabase(context);
        try {
            List<FlashNote> notes = database.getRecentNotes();
            if (notes.isEmpty()) {
                return context.getString(R.string.widget_recent_empty);
            }
            return notes.get(0).getContent();
        } finally {
            database.close();
        }
    }

    private static int pendingIntentFlags() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return flags;
    }
}

package com.dabawei.flashnote;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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

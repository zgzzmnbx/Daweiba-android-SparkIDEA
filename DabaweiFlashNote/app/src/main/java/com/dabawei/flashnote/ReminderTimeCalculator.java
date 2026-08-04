package com.dabawei.flashnote;

import java.util.Calendar;

public final class ReminderTimeCalculator {
    private static final long MILLIS_PER_HOUR = 60L * 60L * 1000L;

    private ReminderTimeCalculator() {
    }

    public static long oneHourAfter(long nowMillis) {
        return nowMillis + MILLIS_PER_HOUR;
    }

    public static long todayAt(long nowMillis, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(nowMillis);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long result = calendar.getTimeInMillis();
        return result > nowMillis ? result : 0L;
    }

    public static long tomorrowAt(long nowMillis, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(nowMillis);
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long atDateAndTime(int year, int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month, day, hour, minute, 0);
        return calendar.getTimeInMillis();
    }

    public static long dayBeforeAt(long dueAt) {
        return preAlertAt(dueAt, ReminderOccurrence.KIND_DAY_BEFORE);
    }

    public static long dayBeforeAt(long dueAt, String dueAtText) {
        if (!isDateOnly(dueAtText)) {
            return dueAt > 0L ? dueAt - 24L * MILLIS_PER_HOUR : 0L;
        }
        return dayBeforeAt(dueAt);
    }

    public static long hourBeforeAt(long dueAt) {
        return preAlertAt(dueAt, ReminderOccurrence.KIND_HOUR_BEFORE);
    }

    public static long hourBeforeAt(long dueAt, String dueAtText) {
        if (!isDateOnly(dueAtText)) {
            return dueAt > 0L ? dueAt - MILLIS_PER_HOUR : 0L;
        }
        return hourBeforeAt(dueAt);
    }

    public static long todayStart(long nowMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(nowMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long todayEnd(long nowMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(todayStart(nowMillis));
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        return calendar.getTimeInMillis() - 1L;
    }

    private static long preAlertAt(long dueAt, String kind) {
        if (dueAt <= 0L) {
            return 0L;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dueAt);
        calendar.set(Calendar.HOUR_OF_DAY, ReminderOccurrence.KIND_DAY_BEFORE.equals(kind) ? 9 : 17);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_YEAR, ReminderOccurrence.KIND_DAY_BEFORE.equals(kind) ? -1 : 0);
        return calendar.getTimeInMillis();
    }

    private static boolean isDateOnly(String dueAtText) {
        return dueAtText != null && dueAtText.trim().length() == TodoDateTime.DATE_PATTERN.length();
    }
}

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
}

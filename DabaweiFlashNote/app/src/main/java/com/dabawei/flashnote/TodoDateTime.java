package com.dabawei.flashnote;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class TodoDateTime {
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm";

    private TodoDateTime() {
    }

    public static long parseDateTime(String value) {
        return parse(value, DATE_TIME_PATTERN);
    }

    public static long parseDate(String value) {
        return parse(value, DATE_PATTERN);
    }

    public static long parseDue(String value) {
        String safeValue = value == null ? "" : value.trim();
        if (safeValue.length() == 0) {
            return 0L;
        }
        return safeValue.length() == DATE_PATTERN.length()
                ? parseDate(safeValue)
                : parseDateTime(safeValue);
    }

    public static String format(long millis) {
        if (millis <= 0L) {
            return "";
        }
        SimpleDateFormat format = new SimpleDateFormat(DATE_TIME_PATTERN, Locale.US);
        format.setLenient(false);
        format.setTimeZone(TimeZone.getDefault());
        return format.format(new Date(millis));
    }

    public static String formatDate(long millis) {
        if (millis <= 0L) {
            return "";
        }
        SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERN, Locale.US);
        format.setLenient(false);
        format.setTimeZone(TimeZone.getDefault());
        return format.format(new Date(millis));
    }

    private static long parse(String value, String pattern) {
        String safeValue = value == null ? "" : value.trim();
        if (safeValue.length() == 0) {
            return 0L;
        }
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
        format.setLenient(false);
        format.setTimeZone(TimeZone.getDefault());
        ParsePosition position = new ParsePosition(0);
        Date parsed = format.parse(safeValue, position);
        if (parsed == null || position.getIndex() != safeValue.length()) {
            return 0L;
        }
        return parsed.getTime();
    }
}

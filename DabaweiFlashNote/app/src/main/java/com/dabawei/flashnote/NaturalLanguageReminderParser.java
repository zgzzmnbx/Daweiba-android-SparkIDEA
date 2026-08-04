package com.dabawei.flashnote;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NaturalLanguageReminderParser {
    private static final Pattern RELATIVE_PATTERN = Pattern.compile(
            "(?<!\\d)(\\d{1,3})\\s*(分钟|小时)后");
    private static final Pattern DAY_PATTERN = Pattern.compile(
            "(今天|明天|后天)\\s*(上午|下午|晚上)?\\s*(\\d{1,2})\\s*点(?:半|\\s*(\\d{1,2})\\s*分?)?");

    private NaturalLanguageReminderParser() {
    }

    public static Candidate parse(String text, long nowMillis) {
        String safeText = text == null ? "" : text.trim();
        if (safeText.length() == 0) {
            return null;
        }

        Matcher relative = RELATIVE_PATTERN.matcher(safeText);
        if (relative.find()) {
            int amount = parseInt(relative.group(1));
            long millis = "小时".equals(relative.group(2))
                    ? amount * 60L * 60L * 1000L
                    : amount * 60L * 1000L;
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(nowMillis);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            calendar.setTimeInMillis(calendar.getTimeInMillis() + millis);
            return new Candidate(calendar.getTimeInMillis(), relative.group());
        }

        Matcher day = DAY_PATTERN.matcher(safeText);
        if (!day.find()) {
            return null;
        }

        int hour = parseInt(day.group(3));
        int minute = day.group(4) == null ? 0 : parseInt(day.group(4));
        if (day.group(0).contains("半")) {
            minute = 30;
        }
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            return null;
        }
        String period = day.group(2);
        if (("下午".equals(period) || "晚上".equals(period)) && hour < 12) {
            hour += 12;
        } else if ("上午".equals(period) && hour == 12) {
            hour = 0;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(nowMillis);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if ("明天".equals(day.group(1))) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        } else if ("后天".equals(day.group(1))) {
            calendar.add(Calendar.DAY_OF_YEAR, 2);
        }
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        long triggerAt = calendar.getTimeInMillis();
        if (triggerAt <= nowMillis) {
            return null;
        }
        return new Candidate(triggerAt, day.group());
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return -1;
        }
    }

    public static final class Candidate {
        private final long triggerAt;
        private final String matchedText;

        private Candidate(long triggerAt, String matchedText) {
            this.triggerAt = triggerAt;
            this.matchedText = matchedText == null ? "" : matchedText;
        }

        public long getTriggerAt() {
            return triggerAt;
        }

        public String getMatchedText() {
            return matchedText;
        }

        public String getDisplayTime() {
            return TodoDateTime.format(triggerAt);
        }
    }
}

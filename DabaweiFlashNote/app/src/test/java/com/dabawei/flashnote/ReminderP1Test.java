package com.dabawei.flashnote;

import java.util.TimeZone;

public final class ReminderP1Test {
    public static void main(String[] args) {
        parsesNaturalLanguageReminderCandidates();
        calculatesMultiLevelReminderTimes();
        keepsOccurrenceNotificationIdsStableAndSeparate();
        System.out.println("Reminder P1 tests passed.");
    }

    private static void parsesNaturalLanguageReminderCandidates() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            long now = TodoDateTime.parseDateTime("2026-08-04 12:00");

            NaturalLanguageReminderParser.Candidate tomorrow =
                    NaturalLanguageReminderParser.parse("明天上午9点提醒我提交报告", now);
            assertEquals(
                    "2026-08-05 09:00",
                    TodoDateTime.format(tomorrow.getTriggerAt()),
                    "tomorrow morning candidate");
            assertEquals("明天上午9点", tomorrow.getMatchedText(), "matched text preserved");

            NaturalLanguageReminderParser.Candidate relative =
                    NaturalLanguageReminderParser.parse("两小时后提醒我", now);
            assertTrue(relative == null, "unsupported Chinese numeral remains unparsed");
            relative = NaturalLanguageReminderParser.parse("2小时后提醒我", now);
            assertEquals("2026-08-04 14:00", TodoDateTime.format(relative.getTriggerAt()), "relative candidate");

            NaturalLanguageReminderParser.Candidate half =
                    NaturalLanguageReminderParser.parse("后天晚上8点半提醒我开会", now);
            assertEquals("2026-08-06 20:30", TodoDateTime.format(half.getTriggerAt()), "half-hour candidate");
            assertTrue(
                    NaturalLanguageReminderParser.parse("把这件事记下来", now) == null,
                    "text without time remains unparsed");
        } finally {
            TimeZone.setDefault(original);
        }
    }

    private static void calculatesMultiLevelReminderTimes() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            long dueAt = TodoDateTime.parseDateTime("2026-08-05 18:00");
            assertEquals(
                    "2026-08-04 18:00",
                    TodoDateTime.format(ReminderTimeCalculator.dayBeforeAt(dueAt, "2026-08-05 18:00")),
                    "day-before reminder");
            assertEquals(
                    "2026-08-05 17:00",
                    TodoDateTime.format(ReminderTimeCalculator.hourBeforeAt(dueAt, "2026-08-05 18:00")),
                    "hour-before reminder");
            long dateOnlyDueAt = TodoDateTime.parseDate("2026-08-05");
            assertEquals(
                    "2026-08-04 09:00",
                    TodoDateTime.format(ReminderTimeCalculator.dayBeforeAt(dateOnlyDueAt, "2026-08-05")),
                    "date-only day-before reminder");
            assertEquals(
                    "2026-08-05 17:00",
                    TodoDateTime.format(ReminderTimeCalculator.hourBeforeAt(dateOnlyDueAt, "2026-08-05")),
                    "date-only hour-before reminder");

            long now = TodoDateTime.parseDateTime("2026-08-05 12:00");
            assertEquals("2026-08-05 00:00", TodoDateTime.format(ReminderTimeCalculator.todayStart(now)), "today start");
            assertEquals("2026-08-05 23:59", TodoDateTime.format(ReminderTimeCalculator.todayEnd(now)), "today end");
        } finally {
            TimeZone.setDefault(original);
        }
    }

    private static void keepsOccurrenceNotificationIdsStableAndSeparate() {
        int dayBefore = ReminderIds.notificationIdForOccurrence(
                "task-1", ReminderOccurrence.KIND_DAY_BEFORE);
        int hourBefore = ReminderIds.notificationIdForOccurrence(
                "task-1", ReminderOccurrence.KIND_HOUR_BEFORE);
        assertEquals(dayBefore, ReminderIds.notificationIdForOccurrence(
                "task-1", ReminderOccurrence.KIND_DAY_BEFORE), "occurrence id stable");
        assertTrue(dayBefore != hourBefore, "occurrence kinds have separate notification ids");
        assertTrue(
                dayBefore != ReminderIds.notificationIdForTaskId("task-1"),
                "occurrence does not reuse primary notification id");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label + ": expected true");
        }
    }
}

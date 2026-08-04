package com.dabawei.flashnote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

public final class ReminderP0Test {
    public static void main(String[] args) {
        parsesAndFormatsProtocolTimes();
        calculatesShortcutTimes();
        keepsNotificationIdsStable();
        plansNewAndChangedReminder();
        preservesLocalReminderOverrideAndFiredState();
        cancelsMissingReminderAndAvoidsOverdueNotification();
        System.out.println("Reminder P0 tests passed.");
    }

    private static void parsesAndFormatsProtocolTimes() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            long parsed = TodoDateTime.parseDateTime("2026-08-05 09:00");
            assertEquals("2026-08-05 09:00", TodoDateTime.format(parsed), "protocol time round trip");
            assertTrue(TodoDateTime.parseDateTime("2026-02-30 09:00") == 0L, "invalid date rejected");
            assertTrue(TodoDateTime.parseDate("2026-08-06") > 0L, "due date parsed");
        } finally {
            TimeZone.setDefault(original);
        }
    }

    private static void calculatesShortcutTimes() {
        long now = 1_754_000_000_000L;
        assertEquals(now + 60L * 60L * 1000L, ReminderTimeCalculator.oneHourAfter(now), "one hour");
        assertTrue(ReminderTimeCalculator.tomorrowAt(now, 9, 0) > now, "tomorrow future");
    }

    private static void keepsNotificationIdsStable() {
        int first = ReminderIds.notificationIdForTaskId("task-a");
        assertEquals(first, ReminderIds.notificationIdForTaskId("task-a"), "notification id stable");
        assertTrue(first > 0, "notification id positive");
        assertEquals("local-note-42", ReminderIds.localTaskId(42L), "local task id");
    }

    private static void plansNewAndChangedReminder() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            long now = TodoDateTime.parseDateTime("2026-08-04 12:00");
            ReminderRecord old = record("task-a", "旧正文", "旧.md", "2026-08-05 09:00", now);
            TodoSyncItem changed = new TodoSyncItem(
                    "新正文",
                    false,
                    "task-a",
                    "新.md",
                    11,
                    "^block-a",
                    "",
                    "2026-08-06",
                    "2026-08-05 10:00");
            ReminderReconciliation.Plan plan = ReminderReconciliation.plan(
                    Collections.singletonList(old),
                    Collections.singletonList(changed),
                    now);
            assertEquals(1, plan.getUpserts().size(), "changed upsert count");
            assertEquals(0, plan.getCancellations().size(), "changed cancellation count");
            assertEquals("新正文", plan.getUpserts().get(0).getTaskText(), "changed text");
            assertEquals("2026-08-05 10:00", plan.getUpserts().get(0).getRemindAtText(), "changed time");
        } finally {
            TimeZone.setDefault(original);
        }
    }

    private static void cancelsMissingReminderAndAvoidsOverdueNotification() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            long now = TodoDateTime.parseDateTime("2026-08-06 12:00");
            ReminderRecord old = record("task-a", "旧正文", "旧.md", "2026-08-05 09:00", now);
            ReminderReconciliation.Plan missing = ReminderReconciliation.plan(
                    Collections.singletonList(old),
                    new ArrayList<TodoSyncItem>(),
                    now);
            assertEquals(1, missing.getCancellations().size(), "missing cancellation count");
            assertEquals(ReminderRecord.STATUS_CANCELLED, missing.getCancellations().get(0).getStatus(), "missing status");

            TodoSyncItem overdue = new TodoSyncItem(
                    "已过期任务",
                    false,
                    "task-overdue",
                    "过期.md",
                    3,
                    "",
                    "",
                    "2026-08-05",
                    "2026-08-05 09:00");
            ReminderReconciliation.Plan overduePlan = ReminderReconciliation.plan(
                    Collections.<ReminderRecord>emptyList(),
                    Arrays.asList(overdue),
                    now);
            assertEquals(1, overduePlan.getOverdueCount(), "overdue count");
            assertEquals(ReminderRecord.STATUS_OVERDUE, overduePlan.getUpserts().get(0).getStatus(), "overdue status");
        } finally {
            TimeZone.setDefault(original);
        }
    }

    private static void preservesLocalReminderOverrideAndFiredState() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            long now = TodoDateTime.parseDateTime("2026-08-04 12:00");
            ReminderRecord localOverride = new ReminderRecord(
                    1L,
                    "task-a",
                    0L,
                    "任务",
                    "工作.md",
                    "^block",
                    0L,
                    TodoDateTime.parseDateTime("2026-08-05 10:00"),
                    0L,
                    ReminderRecord.STATUS_SCHEDULED,
                    ReminderIds.notificationIdForTaskId("task-a"),
                    now,
                    "",
                    "2026-08-05 10:00",
                    "2026-08-05 09:00",
                    "UTC");
            TodoSyncItem unchangedRemote = new TodoSyncItem(
                    "任务",
                    false,
                    "task-a",
                    "工作.md",
                    1,
                    "^block",
                    "",
                    "",
                    "2026-08-05 09:00");
            ReminderReconciliation.Plan overridePlan = ReminderReconciliation.plan(
                    Collections.singletonList(localOverride),
                    Collections.singletonList(unchangedRemote),
                    now);
            assertEquals("2026-08-05 10:00", overridePlan.getUpserts().get(0).getRemindAtText(), "local override preserved");
            assertEquals("2026-08-05 09:00", overridePlan.getUpserts().get(0).getRemoteRemindAtText(), "remote reminder tracked");

            ReminderRecord fired = localOverride.withStatus(ReminderRecord.STATUS_FIRED, 0L);
            ReminderReconciliation.Plan firedPlan = ReminderReconciliation.plan(
                    Collections.singletonList(fired),
                    Collections.singletonList(unchangedRemote),
                    now);
            assertEquals(ReminderRecord.STATUS_FIRED, firedPlan.getUpserts().get(0).getStatus(), "fired state preserved");
        } finally {
            TimeZone.setDefault(original);
        }
    }

    private static ReminderRecord record(String taskId, String text, String source, String remindAtText, long now) {
        return new ReminderRecord(
                1L,
                taskId,
                0L,
                text,
                source,
                "^block",
                0L,
                TodoDateTime.parseDateTime(remindAtText),
                0L,
                ReminderRecord.STATUS_SCHEDULED,
                ReminderIds.notificationIdForTaskId(taskId),
                now,
                "",
                remindAtText,
                TimeZone.getDefault().getID());
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

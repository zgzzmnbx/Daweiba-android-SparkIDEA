package com.dabawei.flashnote;

import java.util.Collections;
import java.util.TimeZone;

public final class ReminderP1Test {
    public static void main(String[] args) {
        parsesNaturalLanguageReminderCandidates();
        parsesAutomaticDateOnlyReminderAtEight();
        parsesNaturalLanguageTableAndRejectsAmbiguity();
        reconcilesStableNaturalRemindersAndSuppression();
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
            assertEquals("2026-08-04 14:00", TodoDateTime.format(relative.getTriggerAt()), "Chinese numeral relative candidate");
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

    private static void parsesAutomaticDateOnlyReminderAtEight() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            long now = TodoDateTime.parseDateTime("2026-08-06 10:15");
            NaturalLanguageReminderParser.Candidate candidate =
                    NaturalLanguageReminderParser.parse("三天后提交汇报", now);
            assertTrue(candidate != null, "date-only natural language candidate");
            assertEquals(
                    "2026-08-09 08:00",
                    TodoDateTime.format(candidate.getTriggerAt()),
                    "date-only default 08:00");
        } finally {
            TimeZone.setDefault(original);
        }
    }

    private static void parsesNaturalLanguageTableAndRejectsAmbiguity() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            long now = TodoDateTime.parseDateTime("2026-08-06 10:15") + 37_000L;
            String[] expressions = new String[]{
                    "30分钟后",
                    "半小时后",
                    "一个小时之后",
                    "两小时以后",
                    "明天",
                    "后天",
                    "大后天",
                    "3天后",
                    "三天后下午2点半",
                    "2天以后8:30",
                    "2026-08-10",
                    "8月12日",
                    "8月12日下午3点",
                    "2026-08-10 18:00",
                    "九十九分钟后"
            };
            String[] expected = new String[]{
                    "2026-08-06 10:45",
                    "2026-08-06 10:45",
                    "2026-08-06 11:15",
                    "2026-08-06 12:15",
                    "2026-08-07 08:00",
                    "2026-08-08 08:00",
                    "2026-08-09 08:00",
                    "2026-08-09 08:00",
                    "2026-08-09 14:30",
                    "2026-08-08 08:30",
                    "2026-08-10 08:00",
                    "2026-08-12 08:00",
                    "2026-08-12 15:00",
                    "2026-08-10 18:00",
                    "2026-08-06 11:54"
            };
            for (int index = 0; index < expressions.length; index++) {
                NaturalLanguageReminderParser.ParseResult result =
                        NaturalLanguageReminderParser.parseResult(expressions[index], now);
                assertTrue(result.isUnique(), "unique expression: " + expressions[index]);
                assertTrue(result.isAutoEligible(), "future expression: " + expressions[index]);
                assertEquals(
                        expected[index],
                        TodoDateTime.format(result.getCandidate().getTriggerAt()),
                        "table expression: " + expressions[index]);
            }

            NaturalLanguageReminderParser.ParseResult metadata =
                    NaturalLanguageReminderParser.parseResult(
                            "三天后提交汇报 (创建日期:: 2026-08-05 09:00)",
                            now);
            assertTrue(metadata.isAutoEligible(), "metadata does not block body expression");
            assertEquals("三天后", metadata.getSourceExpression(), "metadata excluded from source expression");
            assertEquals(
                    TodoDateTime.parseDateTime("2026-08-05 09:00"),
                    NaturalLanguageReminderParser.extractCreationAt(
                            "三天后提交汇报 (创建日期:: 2026-08-05 09:00)"),
                    "creation metadata extracted as baseline");

            assertTrue(!NaturalLanguageReminderParser.parseResult("几天后", now).isUnique(), "几天后 is vague");
            assertTrue(!NaturalLanguageReminderParser.parseResult("三天内完成", now).isUnique(), "三天内 is vague");
            assertTrue(!NaturalLanguageReminderParser.parseResult("最近几天完成", now).isUnique(), "最近几天 is vague");
            assertTrue(
                    !NaturalLanguageReminderParser.parseResult("今天8点", now).isAutoEligible(),
                    "past time is not auto eligible");
            assertTrue(
                    !NaturalLanguageReminderParser.parseResult("2026-08-05", now).isAutoEligible(),
                    "past absolute date is not auto eligible");
            NaturalLanguageReminderParser.ParseResult suppressed =
                    NaturalLanguageReminderParser.parseResult("明天 #不提醒", now);
            assertTrue(suppressed.isSuppressed(), "no-reminder tag suppresses inference");
            assertTrue(!suppressed.isAutoEligible(), "suppressed expression is not eligible");

            NaturalLanguageReminderParser.ParseResult alternatives =
                    NaturalLanguageReminderParser.parseResult("一个或两个小时之后", now);
            assertEquals(2, alternatives.getCandidates().size(), "alternative duration conflict count");
            assertTrue(!alternatives.isUnique(), "alternative duration is not unique");
            assertTrue(
                    NaturalLanguageReminderParser.parse("一个或两个小时之后", now) == null,
                    "ambiguous duration is not auto scheduled");

            NaturalLanguageReminderParser.ParseResult multiple =
                    NaturalLanguageReminderParser.parseResult("明天9点，后天15点", now);
            assertEquals(2, multiple.getCandidates().size(), "multiple time conflict count");
            assertTrue(!multiple.isUnique(), "multiple times are not unique");
            assertTrue(NaturalLanguageReminderParser.parse("明天9点，后天15点", now) == null,
                    "multiple times are not auto scheduled");
        } finally {
            TimeZone.setDefault(original);
        }
    }

    private static void reconcilesStableNaturalRemindersAndSuppression() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            long firstSync = TodoDateTime.parseDateTime("2026-08-06 10:15");
            TodoSyncItem natural = new TodoSyncItem(
                    "三天后提交汇报 #待办",
                    false,
                    "task-natural",
                    "工作.md",
                    10,
                    "^natural",
                    "",
                    "",
                    "");
            ReminderReconciliation.Plan first = ReminderReconciliation.plan(
                    Collections.<ReminderRecord>emptyList(),
                    Collections.singletonList(natural),
                    firstSync);
            assertEquals(1, first.getUpserts().size(), "first natural upsert count");
            ReminderRecord firstRecord = first.getUpserts().get(0);
            assertEquals("2026-08-09 08:00", TodoDateTime.format(firstRecord.getRemindAt()), "first natural time");
            assertEquals(ReminderRecord.SOURCE_NATURAL, firstRecord.getReminderSource(), "natural source stored");
            assertEquals("三天后", firstRecord.getSourceExpression(), "natural expression stored");
            assertEquals(firstSync, firstRecord.getNaturalReferenceAt(), "fallback baseline stored");

            ReminderReconciliation.Plan second = ReminderReconciliation.plan(
                    Collections.singletonList(firstRecord),
                    Collections.singletonList(natural),
                    firstSync + 24L * 60L * 60L * 1000L);
            ReminderRecord secondRecord = second.getUpserts().get(0);
            assertEquals(firstRecord.getRemindAt(), secondRecord.getRemindAt(), "second sync does not drift");
            assertEquals(firstRecord.getSourceSignature(), secondRecord.getSourceSignature(), "signature stable");

            ReminderReconciliation.Plan third = ReminderReconciliation.plan(
                    Collections.singletonList(secondRecord),
                    Collections.singletonList(natural),
                    firstSync + 2L * 24L * 60L * 60L * 1000L);
            assertEquals(
                    secondRecord.getRemindAt(),
                    third.getUpserts().get(0).getRemindAt(),
                    "third sync does not drift");

            ReminderRecord cancelled = firstRecord.withStatus(ReminderRecord.STATUS_CANCELLED, 0L)
                    .withAutoSuppressed(true);
            ReminderReconciliation.Plan suppressed = ReminderReconciliation.plan(
                    Collections.singletonList(cancelled),
                    Collections.singletonList(natural),
                    firstSync + 3L * 24L * 60L * 60L * 1000L);
            assertEquals(1, suppressed.getUpserts().size(), "suppressed task retained");
            assertEquals(ReminderRecord.STATUS_CANCELLED, suppressed.getUpserts().get(0).getStatus(),
                    "suppressed task does not resurrect");
            assertTrue(suppressed.getUpserts().get(0).isAutoSuppressed(), "suppression flag retained");

            TodoSyncItem changedExpression = new TodoSyncItem(
                    "四天后提交汇报 #待办",
                    false,
                    "task-natural",
                    "工作.md",
                    10,
                    "^natural",
                    "",
                    "",
                    "");
            ReminderReconciliation.Plan changed = ReminderReconciliation.plan(
                    Collections.singletonList(cancelled),
                    Collections.singletonList(changedExpression),
                    firstSync + 3L * 24L * 60L * 60L * 1000L);
            assertEquals(ReminderRecord.STATUS_SCHEDULED, changed.getUpserts().get(0).getStatus(),
                    "changed expression creates a new reminder");
            assertEquals("2026-08-10 08:00", TodoDateTime.format(changed.getUpserts().get(0).getRemindAt()),
                    "changed expression uses stable baseline");

            TodoSyncItem dueOnly = new TodoSyncItem(
                    "完成月度统计 #待办",
                    false,
                    "task-due",
                    "工作.md",
                    11,
                    "^due",
                    "",
                    "2026-08-10",
                    "");
            ReminderRecord dueRecord = ReminderReconciliation.plan(
                    Collections.<ReminderRecord>emptyList(),
                    Collections.singletonList(dueOnly),
                    firstSync).getUpserts().get(0);
            assertEquals(ReminderRecord.SOURCE_DUE_DEFAULT, dueRecord.getReminderSource(), "due default source");
            assertEquals("2026-08-10 08:00", TodoDateTime.format(dueRecord.getRemindAt()), "due default time");

            TodoSyncItem past = new TodoSyncItem(
                    "2026-08-05 提交汇报 #待办",
                    false,
                    "task-past",
                    "工作.md",
                    11,
                    "^past",
                    "",
                    "",
                    "");
            ReminderRecord pastRecord = ReminderReconciliation.plan(
                    Collections.<ReminderRecord>emptyList(),
                    Collections.singletonList(past),
                    firstSync).getUpserts().get(0);
            assertEquals(ReminderRecord.STATUS_OVERDUE, pastRecord.getStatus(), "past natural time is overdue");

            TodoSyncItem explicit = new TodoSyncItem(
                    "三天后提交汇报 #待办",
                    false,
                    "task-explicit",
                    "工作.md",
                    12,
                    "^explicit",
                    "",
                    "",
                    "2026-08-07 09:00");
            ReminderRecord explicitRecord = ReminderReconciliation.plan(
                    Collections.<ReminderRecord>emptyList(),
                    Collections.singletonList(explicit),
                    firstSync).getUpserts().get(0);
            assertEquals(ReminderRecord.SOURCE_EXPLICIT, explicitRecord.getReminderSource(), "explicit priority source");
            assertEquals("2026-08-07 09:00", TodoDateTime.format(explicitRecord.getRemindAt()), "explicit priority time");

            TodoSyncItem conflict = new TodoSyncItem(
                    "明天9点，后天15点 #待办",
                    false,
                    "task-conflict",
                    "工作.md",
                    13,
                    "^conflict",
                    "",
                    "",
                    "");
            ReminderReconciliation.Plan conflictPlan = ReminderReconciliation.plan(
                    Collections.<ReminderRecord>emptyList(),
                    Collections.singletonList(conflict),
                    firstSync);
            assertEquals(0, conflictPlan.getUpserts().size(), "conflict has no automatic upsert");
            assertEquals(1, conflictPlan.getConflictCount(), "conflict is reported");
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

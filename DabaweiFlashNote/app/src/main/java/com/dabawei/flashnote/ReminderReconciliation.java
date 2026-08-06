package com.dabawei.flashnote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public final class ReminderReconciliation {
    private ReminderReconciliation() {
    }

    public static Plan plan(List<ReminderRecord> existing, List<TodoSyncItem> incoming, long nowMillis) {
        Map<String, ReminderRecord> existingByTaskId = new HashMap<>();
        if (existing != null) {
            for (ReminderRecord record : existing) {
                if (record != null && record.getTaskId().length() > 0) {
                    existingByTaskId.put(record.getTaskId(), record);
                }
            }
        }

        ArrayList<ReminderRecord> upserts = new ArrayList<>();
        ArrayList<ReminderRecord> cancellations = new ArrayList<>();
        ArrayList<String> conflicts = new ArrayList<>();
        Set<String> seenTaskIds = new HashSet<>();
        int overdueCount = 0;
        if (incoming != null) {
            for (TodoSyncItem item : incoming) {
                if (item == null) {
                    continue;
                }
                String taskId = item.getTaskId();
                if (taskId == null || taskId.trim().length() == 0) {
                    taskId = ReminderIds.generatedTaskId(item.getSourcePath(), item.getLineNumber(), item.getText());
                }
                taskId = taskId.trim();
                seenTaskIds.add(taskId);
                ReminderRecord old = existingByTaskId.get(taskId);

                if (item.isDone() || NaturalLanguageReminderParser.containsNoReminderTag(item.getText())) {
                    if (item.isDone()) {
                        addCancellation(cancellations, old, false);
                    } else {
                        addCancellation(cancellations, old, true);
                    }
                    continue;
                }

                long dueAt = TodoDateTime.parseDue(item.getDueAtText());
                String explicitText = safe(item.getRemindAtText());
                long explicitAt = TodoDateTime.parseDateTime(explicitText);
                if (explicitText.length() > 0) {
                    if (explicitAt <= 0L) {
                        addCancellation(cancellations, old, false);
                        continue;
                    }
                    ReminderRecord preserved = preserveRemoteExplicitOverride(
                            old,
                            item,
                            taskId,
                            dueAt,
                            explicitText,
                            nowMillis);
                    if (preserved != null) {
                        upserts.add(preserved);
                        continue;
                    }
                    ReminderRecord record = buildAutomaticRecord(
                            old,
                            taskId,
                            item,
                            dueAt,
                            explicitAt,
                            explicitText,
                            ReminderRecord.SOURCE_EXPLICIT,
                            explicitText,
                            buildSignature(taskId, ReminderRecord.SOURCE_EXPLICIT, explicitText, 0L),
                            0L,
                            nowMillis,
                            false);
                    upserts.add(record);
                    if (ReminderRecord.STATUS_OVERDUE.equals(record.getStatus())
                            && (old == null || !ReminderRecord.STATUS_OVERDUE.equals(old.getStatus()))) {
                        overdueCount++;
                    }
                    continue;
                }

                long naturalReferenceAt = chooseNaturalReferenceAt(old, item.getText(), nowMillis);
                NaturalLanguageReminderParser.ParseResult natural =
                        NaturalLanguageReminderParser.parseResult(item.getText(), naturalReferenceAt);
                if (natural.getCandidates().size() > 1) {
                    conflicts.add(taskId);
                }
                if (natural.isAutoEligible()) {
                    NaturalLanguageReminderParser.Candidate candidate = natural.getCandidate();
                    String expression = natural.getSourceExpression();
                    String signature = buildSignature(
                            taskId,
                            ReminderRecord.SOURCE_NATURAL,
                            expression,
                            naturalReferenceAt);
                    if (isManualOverride(old)) {
                        upserts.add(preserveManualReminder(old, item, taskId, dueAt, nowMillis));
                        continue;
                    }
                    boolean sameNatural = old != null
                            && ReminderRecord.SOURCE_NATURAL.equals(old.getReminderSource())
                            && signature.equals(old.getSourceSignature())
                            && expression.equals(old.getSourceExpression());
                    long triggerAt = sameNatural ? old.getRemindAt() : candidate.getTriggerAt();
                    String status = resolveStatus(old, triggerAt, nowMillis, sameNatural);
                    ReminderRecord record = buildRecord(
                            old,
                            taskId,
                            item,
                            dueAt,
                            triggerAt,
                            status,
                            ReminderRecord.SOURCE_NATURAL,
                            expression,
                            signature,
                            naturalReferenceAt,
                            old != null && sameNatural && old.isAutoSuppressed(),
                            nowMillis);
                    upserts.add(record);
                    if (ReminderRecord.STATUS_OVERDUE.equals(record.getStatus())
                            && (old == null || !ReminderRecord.STATUS_OVERDUE.equals(old.getStatus()))) {
                        overdueCount++;
                    }
                    continue;
                }

                if (natural.isUnique() && natural.getCandidate() != null) {
                    NaturalLanguageReminderParser.Candidate pastCandidate = natural.getCandidate();
                    String expression = natural.getSourceExpression();
                    String signature = buildSignature(
                            taskId,
                            ReminderRecord.SOURCE_NATURAL,
                            expression,
                            naturalReferenceAt);
                    if (isManualOverride(old)) {
                        upserts.add(preserveManualReminder(old, item, taskId, dueAt, nowMillis));
                        continue;
                    }
                    boolean sameNatural = old != null
                            && ReminderRecord.SOURCE_NATURAL.equals(old.getReminderSource())
                            && signature.equals(old.getSourceSignature())
                            && expression.equals(old.getSourceExpression());
                    long triggerAt = sameNatural ? old.getRemindAt() : pastCandidate.getTriggerAt();
                    String status = resolveStatus(old, triggerAt, nowMillis, sameNatural);
                    ReminderRecord record = buildRecord(
                            old,
                            taskId,
                            item,
                            dueAt,
                            triggerAt,
                            status,
                            ReminderRecord.SOURCE_NATURAL,
                            expression,
                            signature,
                            naturalReferenceAt,
                            old != null && sameNatural && old.isAutoSuppressed(),
                            nowMillis);
                    upserts.add(record);
                    if (ReminderRecord.STATUS_OVERDUE.equals(record.getStatus())
                            && (old == null || !ReminderRecord.STATUS_OVERDUE.equals(old.getStatus()))) {
                        overdueCount++;
                    }
                    continue;
                }

                if (dueAt > 0L) {
                    long dueDefaultAt = ReminderTimeCalculator.dueDefaultAt(dueAt);
                    String dueExpression = safe(item.getDueAtText());
                    String dueSignature = buildSignature(
                            taskId,
                            ReminderRecord.SOURCE_DUE_DEFAULT,
                            dueExpression,
                            0L);
                    if (isManualOverride(old)) {
                        upserts.add(preserveManualReminder(old, item, taskId, dueAt, nowMillis));
                        continue;
                    }
                    boolean sameDue = old != null
                            && ReminderRecord.SOURCE_DUE_DEFAULT.equals(old.getReminderSource())
                            && dueSignature.equals(old.getSourceSignature())
                            && dueExpression.equals(old.getSourceExpression());
                    long triggerAt = sameDue ? old.getRemindAt() : dueDefaultAt;
                    String status = resolveStatus(old, triggerAt, nowMillis, sameDue);
                    ReminderRecord record = buildRecord(
                            old,
                            taskId,
                            item,
                            dueAt,
                            triggerAt,
                            status,
                            ReminderRecord.SOURCE_DUE_DEFAULT,
                            dueExpression,
                            dueSignature,
                            0L,
                            old != null && sameDue && old.isAutoSuppressed(),
                            nowMillis);
                    upserts.add(record);
                    if (ReminderRecord.STATUS_OVERDUE.equals(record.getStatus())
                            && (old == null || !ReminderRecord.STATUS_OVERDUE.equals(old.getStatus()))) {
                        overdueCount++;
                    }
                    continue;
                }

                addCancellation(cancellations, old, false);
            }
        }

        for (ReminderRecord old : existingByTaskId.values()) {
            if (!seenTaskIds.contains(old.getTaskId())) {
                addCancellation(cancellations, old, old.isAutoSuppressed());
            }
        }
        return new Plan(upserts, cancellations, overdueCount, conflicts);
    }

    private static ReminderRecord preserveRemoteExplicitOverride(
            ReminderRecord old,
            TodoSyncItem item,
            String taskId,
            long dueAt,
            String explicitText,
            long nowMillis) {
        if (old == null || !explicitText.equals(old.getRemoteRemindAtText())) {
            return null;
        }
        boolean localOverride = old.getRemindAt() != TodoDateTime.parseDateTime(explicitText)
                && !ReminderRecord.SOURCE_NATURAL.equals(old.getReminderSource())
                && !ReminderRecord.SOURCE_DUE_DEFAULT.equals(old.getReminderSource());
        boolean completedState = ReminderRecord.STATUS_CANCELLED.equals(old.getStatus())
                || ReminderRecord.STATUS_FIRED.equals(old.getStatus())
                || ReminderRecord.STATUS_OVERDUE.equals(old.getStatus());
        if (!localOverride && !completedState) {
            return null;
        }
        long preservedRemindAt = old.getRemindAt();
        String status = resolveStatus(old, preservedRemindAt, nowMillis, true);
        return buildRecord(
                old,
                taskId,
                item,
                dueAt,
                preservedRemindAt,
                status,
                old.getReminderSource().length() == 0 ? ReminderRecord.SOURCE_EXPLICIT : old.getReminderSource(),
                old.getSourceExpression().length() == 0 ? explicitText : old.getSourceExpression(),
                old.getSourceSignature().length() == 0
                        ? buildSignature(taskId, ReminderRecord.SOURCE_EXPLICIT, explicitText, 0L)
                        : old.getSourceSignature(),
                old.getNaturalReferenceAt(),
                old.isAutoSuppressed(),
                nowMillis,
                explicitText);
    }

    private static ReminderRecord preserveManualReminder(
            ReminderRecord old,
            TodoSyncItem item,
            String taskId,
            long dueAt,
            long nowMillis) {
        String status = resolveStatus(old, old.getRemindAt(), nowMillis, true);
        return buildRecord(
                old,
                taskId,
                item,
                dueAt,
                old.getRemindAt(),
                status,
                ReminderRecord.SOURCE_MANUAL,
                old.getSourceExpression(),
                old.getSourceSignature(),
                old.getNaturalReferenceAt(),
                old.isAutoSuppressed(),
                nowMillis,
                old.getRemoteRemindAtText());
    }

    private static ReminderRecord buildAutomaticRecord(
            ReminderRecord old,
            String taskId,
            TodoSyncItem item,
            long dueAt,
            long remindAt,
            String remindAtText,
            String source,
            String expression,
            String signature,
            long naturalReferenceAt,
            long nowMillis,
            boolean suppressed) {
        boolean sameSignature = old != null && signature.equals(old.getSourceSignature());
        String status = resolveStatus(old, remindAt, nowMillis, sameSignature);
        return buildRecord(
                old,
                taskId,
                item,
                dueAt,
                remindAt,
                status,
                source,
                expression,
                signature,
                naturalReferenceAt,
                suppressed && sameSignature,
                nowMillis,
                remindAtText);
    }

    private static ReminderRecord buildRecord(
            ReminderRecord old,
            String taskId,
            TodoSyncItem item,
            long dueAt,
            long remindAt,
            String status,
            String source,
            String expression,
            String signature,
            long naturalReferenceAt,
            boolean autoSuppressed,
            long nowMillis) {
        return buildRecord(
                old,
                taskId,
                item,
                dueAt,
                remindAt,
                status,
                source,
                expression,
                signature,
                naturalReferenceAt,
                autoSuppressed,
                nowMillis,
                source.equals(ReminderRecord.SOURCE_EXPLICIT) ? expression : "");
    }

    private static ReminderRecord buildRecord(
            ReminderRecord old,
            String taskId,
            TodoSyncItem item,
            long dueAt,
            long remindAt,
            String status,
            String source,
            String expression,
            String signature,
            long naturalReferenceAt,
            boolean autoSuppressed,
            long nowMillis,
            String remoteRemindAtText) {
        int notificationId = old == null
                ? ReminderIds.notificationIdForTaskId(taskId)
                : old.getNotificationId();
        return new ReminderRecord(
                old == null ? 0L : old.getReminderId(),
                taskId,
                0L,
                item.getText(),
                item.getSourcePath(),
                item.getBlockId(),
                dueAt,
                remindAt,
                old == null ? 0L : old.getSnoozeUntil(),
                status,
                notificationId,
                nowMillis,
                item.getDueAtText(),
                TodoDateTime.format(remindAt),
                remoteRemindAtText,
                TimeZone.getDefault().getID(),
                source,
                expression,
                signature,
                naturalReferenceAt,
                autoSuppressed);
    }

    private static String resolveStatus(
            ReminderRecord old,
            long remindAt,
            long nowMillis,
            boolean sameSignature) {
        if (sameSignature && old != null) {
            if (ReminderRecord.STATUS_CANCELLED.equals(old.getStatus()) && old.isAutoSuppressed()) {
                return ReminderRecord.STATUS_CANCELLED;
            }
            if (ReminderRecord.STATUS_FIRED.equals(old.getStatus())) {
                return ReminderRecord.STATUS_FIRED;
            }
            if (ReminderRecord.STATUS_OVERDUE.equals(old.getStatus()) && remindAt <= nowMillis) {
                return ReminderRecord.STATUS_OVERDUE;
            }
            if (old.getSnoozeUntil() > nowMillis && remindAt > nowMillis) {
                return ReminderRecord.STATUS_SNOOZED;
            }
        }
        return remindAt > nowMillis
                ? ReminderRecord.STATUS_SCHEDULED
                : ReminderRecord.STATUS_OVERDUE;
    }

    private static long chooseNaturalReferenceAt(
            ReminderRecord old,
            String taskText,
            long nowMillis) {
        long creationAt = NaturalLanguageReminderParser.extractCreationAt(taskText);
        if (creationAt > 0L) {
            return creationAt;
        }
        if (old != null && old.getNaturalReferenceAt() > 0L) {
            return old.getNaturalReferenceAt();
        }
        return nowMillis;
    }

    private static boolean isManualOverride(ReminderRecord old) {
        return old != null
                && ReminderRecord.SOURCE_MANUAL.equals(old.getReminderSource())
                && !ReminderRecord.STATUS_CANCELLED.equals(old.getStatus())
                && old.getRemindAt() > 0L;
    }

    private static void addCancellation(
            List<ReminderRecord> cancellations,
            ReminderRecord old,
            boolean suppress) {
        if (old == null) {
            return;
        }
        boolean changed = !ReminderRecord.STATUS_CANCELLED.equals(old.getStatus())
                || (suppress && !old.isAutoSuppressed());
        if (changed) {
            cancellations.add(old.withStatus(ReminderRecord.STATUS_CANCELLED, 0L)
                    .withAutoSuppressed(old.isAutoSuppressed() || suppress));
        }
    }

    private static String buildSignature(
            String taskId,
            String source,
            String expression,
            long referenceAt) {
        return safe(taskId) + "|" + safe(source) + "|" + safe(expression) + "|" + referenceAt;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Plan {
        private final List<ReminderRecord> upserts;
        private final List<ReminderRecord> cancellations;
        private final int overdueCount;
        private final List<String> conflictTaskIds;

        private Plan(
                List<ReminderRecord> upserts,
                List<ReminderRecord> cancellations,
                int overdueCount,
                List<String> conflictTaskIds) {
            this.upserts = upserts;
            this.cancellations = cancellations;
            this.overdueCount = overdueCount;
            this.conflictTaskIds = Collections.unmodifiableList(new ArrayList<>(conflictTaskIds));
        }

        public List<ReminderRecord> getUpserts() {
            return upserts;
        }

        public List<ReminderRecord> getCancellations() {
            return cancellations;
        }

        public int getOverdueCount() {
            return overdueCount;
        }

        public List<String> getConflictTaskIds() {
            return conflictTaskIds;
        }

        public int getConflictCount() {
            return conflictTaskIds.size();
        }
    }
}

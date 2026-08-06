package com.dabawei.flashnote;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses only concrete, future-capable reminder expressions from todo text.
 * The caller supplies the stable reference time, so relative expressions do
 * not silently move when the same Obsidian task is synchronized again.
 */
public final class NaturalLanguageReminderParser {
    private static final String NUMBER_TOKEN =
            "(?:[0-9]{1,2}|[零〇一二两三四五六七八九十]+|一个|两个)";
    private static final String NUMBER_BOUNDARY =
            "[0-9零〇一二两三四五六七八九十两一个两个大]";
    private static final String PERIOD = "上午|中午|下午|晚上";
    private static final String TIME_TAIL =
            "\\s*(?:" + PERIOD + ")?\\s*(?:(?:" + NUMBER_TOKEN
                    + ")\\s*点(?:\\s*(?:半|" + NUMBER_TOKEN + "\\s*分?))?"
                    + "|(?:" + NUMBER_TOKEN + ")\\s*[:：]\\s*(?:" + NUMBER_TOKEN + "))?";

    private static final Pattern ALTERNATIVE_RELATIVE_PATTERN = Pattern.compile(
            "(?<!" + NUMBER_BOUNDARY + ")(" + NUMBER_TOKEN + ")\\s*或\\s*("
                    + NUMBER_TOKEN + ")\\s*(分钟|小时)\\s*(后|之后|以后)");
    private static final Pattern HALF_HOUR_PATTERN = Pattern.compile(
            "(?<!" + NUMBER_BOUNDARY + ")半\\s*小时\\s*(后|之后|以后)");
    private static final Pattern RELATIVE_PATTERN = Pattern.compile(
            "(?<!" + NUMBER_BOUNDARY + ")(" + NUMBER_TOKEN + ")\\s*(分钟|小时)\\s*(后|之后|以后)");
    private static final Pattern DAY_PATTERN = Pattern.compile(
            "(?<!" + NUMBER_BOUNDARY + ")(?:(大后天|后天|明天|今天)|(" + NUMBER_TOKEN
                    + ")\\s*天\\s*(后|之后|以后))(" + TIME_TAIL + ")");
    private static final Pattern ISO_DATE_TIME_PATTERN = Pattern.compile(
            "(?<![0-9])(\\d{4})-(\\d{1,2})-(\\d{1,2})\\s+"
                    + "(?:(上午|中午|下午|晚上)\\s*)?(\\d{1,2})\\s*(?:[:：点]\\s*(\\d{1,2}))?");
    private static final Pattern ISO_DATE_PATTERN = Pattern.compile(
            "(?<![0-9])(\\d{4})-(\\d{1,2})-(\\d{1,2})");
    private static final Pattern MONTH_DATE_PATTERN = Pattern.compile(
            "(?<![0-9])((\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*日)(" + TIME_TAIL + ")");
    private static final Pattern CLOCK_PATTERN = Pattern.compile(
            "^\\s*(上午|中午|下午|晚上)?\\s*(?:(" + NUMBER_TOKEN
                    + ")\\s*点\\s*(半|" + NUMBER_TOKEN + "\\s*分?)?|("
                    + NUMBER_TOKEN + ")\\s*[:：]\\s*(" + NUMBER_TOKEN + "))\\s*$");
    private static final Pattern METADATA_PATTERN = Pattern.compile(
            "(?:创建日期|记录日期|同步时间|来源文件|行号|块ID|任务ID|截止日期|提醒时间|备注)"
                    + "\\s*::\\s*[^\\n\\)\\]\\|]*");
    private static final Pattern INLINE_DUE_PATTERN = Pattern.compile(
            "(?:📅|(?i:due::))\\s*\\d{4}-\\d{1,2}-\\d{1,2}(?:\\s+\\d{1,2}[:：]\\d{1,2})?");
    private static final Pattern NO_REMINDER_PATTERN = Pattern.compile(
            "(?<!\\S)#不提醒(?![\\p{L}\\p{N}_-])");
    private static final Pattern CREATION_PATTERN = Pattern.compile(
            "创建日期\\s*::\\s*(\\d{4}-\\d{1,2}-\\d{1,2}(?:\\s+\\d{1,2}[:：]\\d{1,2})?)");

    private NaturalLanguageReminderParser() {
    }

    /**
     * Compatibility helper for the P0/P1 UI. It returns a candidate only if
     * the expression is unique, future, and not suppressed.
     */
    public static Candidate parse(String text, long referenceAt) {
        ParseResult result = parseResult(text, referenceAt);
        return result.isAutoEligible() ? result.getCandidate() : null;
    }

    public static ParseResult parseResult(String text, long referenceAt) {
        String safeText = text == null ? "" : text.trim();
        if (safeText.length() == 0) {
            return ParseResult.empty(referenceAt);
        }
        if (containsNoReminderTag(safeText)) {
            return ParseResult.suppressed(referenceAt);
        }

        String searchableText = removeMetadata(safeText);
        ArrayList<RawMatch> rawMatches = new ArrayList<>();
        collectAlternativeRelativeMatches(searchableText, referenceAt, rawMatches);
        collectRelativeMatches(searchableText, referenceAt, rawMatches);
        collectDayMatches(searchableText, referenceAt, rawMatches);
        collectAbsoluteMatches(searchableText, referenceAt, rawMatches);

        List<RawMatch> selected = selectNonOverlapping(rawMatches);
        ArrayList<Candidate> candidates = new ArrayList<>();
        for (RawMatch match : selected) {
            if (match != null && match.candidate != null) {
                candidates.add(match.candidate);
            }
        }
        String sourceExpression = candidates.size() == 1
                ? candidates.get(0).getMatchedText()
                : joinExpressions(candidates);
        return new ParseResult(candidates, referenceAt, sourceExpression, false);
    }

    public static boolean containsNoReminderTag(String text) {
        return text != null && NO_REMINDER_PATTERN.matcher(text).find();
    }

    public static long extractCreationAt(String text) {
        if (text == null || text.trim().length() == 0) {
            return 0L;
        }
        Matcher matcher = CREATION_PATTERN.matcher(text);
        if (!matcher.find()) {
            return 0L;
        }
        String value = matcher.group(1);
        long parsed = value.indexOf(' ') >= 0 || value.indexOf('：') >= 0
                ? TodoDateTime.parseDateTime(value.replace('：', ':'))
                : TodoDateTime.parseDate(value);
        return parsed > 0L ? parsed : 0L;
    }

    private static void collectAlternativeRelativeMatches(
            String text,
            long referenceAt,
            ArrayList<RawMatch> output) {
        Matcher matcher = ALTERNATIVE_RELATIVE_PATTERN.matcher(text);
        while (matcher.find()) {
            int first = parseNumber(matcher.group(1));
            int second = parseNumber(matcher.group(2));
            long firstDuration = durationMillis(first, matcher.group(3));
            long secondDuration = durationMillis(second, matcher.group(3));
            if (firstDuration > 0L) {
                output.add(new RawMatch(
                        matcher.start(),
                        matcher.end(),
                        new Candidate(referenceAt + firstDuration, matcher.group(), referenceAt)));
            }
            if (secondDuration > 0L) {
                output.add(new RawMatch(
                        matcher.start(),
                        matcher.end(),
                        new Candidate(referenceAt + secondDuration, matcher.group(), referenceAt)));
            }
        }
    }

    private static void collectRelativeMatches(
            String text,
            long referenceAt,
            ArrayList<RawMatch> output) {
        Matcher halfHour = HALF_HOUR_PATTERN.matcher(text);
        while (halfHour.find()) {
            output.add(new RawMatch(
                    halfHour.start(),
                    halfHour.end(),
                    new Candidate(referenceAt + 30L * 60L * 1000L, halfHour.group(), referenceAt)));
        }

        Matcher matcher = RELATIVE_PATTERN.matcher(text);
        while (matcher.find()) {
            int amount = parseNumber(matcher.group(1));
            long duration = durationMillis(amount, matcher.group(2));
            if (duration <= 0L) {
                continue;
            }
            output.add(new RawMatch(
                    matcher.start(),
                    matcher.end(),
                    new Candidate(referenceAt + duration, matcher.group(), referenceAt)));
        }
    }

    private static void collectDayMatches(
            String text,
            long referenceAt,
            ArrayList<RawMatch> output) {
        Matcher matcher = DAY_PATTERN.matcher(text);
        while (matcher.find()) {
            int dayOffset;
            if (matcher.group(1) != null) {
                if ("今天".equals(matcher.group(1))) {
                    dayOffset = 0;
                } else if ("明天".equals(matcher.group(1))) {
                    dayOffset = 1;
                } else if ("后天".equals(matcher.group(1))) {
                    dayOffset = 2;
                } else {
                    dayOffset = 3;
                }
            } else {
                dayOffset = parseNumber(matcher.group(2));
            }
            if (dayOffset < 0) {
                continue;
            }
            Clock clock = parseClockTail(matcher.group(4));
            if (clock == null) {
                continue;
            }
            long triggerAt = atRelativeDate(referenceAt, dayOffset, clock.hour, clock.minute);
            if (triggerAt <= 0L) {
                continue;
            }
            output.add(new RawMatch(
                    matcher.start(),
                    matcher.end(),
                    new Candidate(triggerAt, matcher.group(), referenceAt)));
        }
    }

    private static void collectAbsoluteMatches(
            String text,
            long referenceAt,
            ArrayList<RawMatch> output) {
        Matcher isoDateTime = ISO_DATE_TIME_PATTERN.matcher(text);
        while (isoDateTime.find()) {
            int year = parseInt(isoDateTime.group(1));
            int month = parseInt(isoDateTime.group(2));
            int day = parseInt(isoDateTime.group(3));
            int hour = parseInt(isoDateTime.group(5));
            int minute = isoDateTime.group(6) == null ? 0 : parseInt(isoDateTime.group(6));
            hour = applyPeriod(hour, isoDateTime.group(4));
            long triggerAt = atDate(referenceAt, year, month, day, hour, minute);
            if (triggerAt > 0L) {
                output.add(new RawMatch(
                        isoDateTime.start(),
                        isoDateTime.end(),
                        new Candidate(triggerAt, isoDateTime.group(), referenceAt)));
            }
        }

        Matcher isoDate = ISO_DATE_PATTERN.matcher(text);
        while (isoDate.find()) {
            int year = parseInt(isoDate.group(1));
            int month = parseInt(isoDate.group(2));
            int day = parseInt(isoDate.group(3));
            long triggerAt = atDate(referenceAt, year, month, day, 8, 0);
            if (triggerAt > 0L) {
                output.add(new RawMatch(
                        isoDate.start(),
                        isoDate.end(),
                        new Candidate(triggerAt, isoDate.group(), referenceAt)));
            }
        }

        Matcher monthDate = MONTH_DATE_PATTERN.matcher(text);
        while (monthDate.find()) {
            int month = parseInt(monthDate.group(2));
            int day = parseInt(monthDate.group(3));
            Clock clock = parseClockTail(monthDate.group(4));
            if (clock == null) {
                continue;
            }
            Calendar reference = calendar(referenceAt);
            long triggerAt = atDate(
                    referenceAt,
                    reference.get(Calendar.YEAR),
                    month,
                    day,
                    clock.hour,
                    clock.minute);
            if (triggerAt > 0L) {
                output.add(new RawMatch(
                        monthDate.start(),
                        monthDate.end(),
                        new Candidate(triggerAt, monthDate.group(), referenceAt)));
            }
        }
    }

    private static List<RawMatch> selectNonOverlapping(List<RawMatch> rawMatches) {
        if (rawMatches == null || rawMatches.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<RawMatch> sorted = new ArrayList<>(rawMatches);
        Collections.sort(sorted, new Comparator<RawMatch>() {
            @Override
            public int compare(RawMatch left, RawMatch right) {
                int byStart = Integer.compare(left.start, right.start);
                if (byStart != 0) {
                    return byStart;
                }
                return Integer.compare(right.end - right.start, left.end - left.start);
            }
        });
        ArrayList<RawMatch> selected = new ArrayList<>();
        for (RawMatch candidate : sorted) {
            if (selected.isEmpty()) {
                selected.add(candidate);
                continue;
            }
            RawMatch previous = selected.get(selected.size() - 1);
            if (candidate.start < previous.end) {
                boolean sameSpan = candidate.start == previous.start && candidate.end == previous.end;
                if (candidate.end - candidate.start > previous.end - previous.start) {
                    selected.set(selected.size() - 1, candidate);
                } else if (sameSpan && candidate.candidate.getTriggerAt() != previous.candidate.getTriggerAt()) {
                    selected.add(candidate);
                }
            } else {
                selected.add(candidate);
            }
        }
        Collections.sort(selected, new Comparator<RawMatch>() {
            @Override
            public int compare(RawMatch left, RawMatch right) {
                return Integer.compare(left.start, right.start);
            }
        });
        return selected;
    }

    private static String joinExpressions(List<Candidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Candidate candidate : candidates) {
            if (candidate == null || candidate.getMatchedText().length() == 0
                    || builder.indexOf(candidate.getMatchedText()) >= 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("；");
            }
            builder.append(candidate.getMatchedText());
        }
        return builder.toString();
    }

    private static String removeMetadata(String text) {
        String searchable = METADATA_PATTERN.matcher(text).replaceAll(" ");
        return INLINE_DUE_PATTERN.matcher(searchable).replaceAll(" ");
    }

    private static Clock parseClockTail(String tail) {
        if (tail == null || tail.trim().length() == 0) {
            return new Clock(8, 0);
        }
        Matcher matcher = CLOCK_PATTERN.matcher(tail);
        if (!matcher.matches()) {
            return null;
        }
        String period = matcher.group(1);
        String hourText = matcher.group(2) != null ? matcher.group(2) : matcher.group(4);
        String minuteText = matcher.group(3) != null ? matcher.group(3) : matcher.group(5);
        int hour = parseNumber(hourText);
        int minute;
        if (minuteText == null || minuteText.trim().length() == 0) {
            minute = 0;
        } else if ("半".equals(minuteText.trim())) {
            minute = 30;
        } else {
            minute = parseNumber(minuteText.replace("分", ""));
        }
        hour = applyPeriod(hour, period);
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            return null;
        }
        return new Clock(hour, minute);
    }

    private static int applyPeriod(int hour, String period) {
        if (hour < 0) {
            return -1;
        }
        if (("下午".equals(period) || "晚上".equals(period) || "中午".equals(period)) && hour < 12) {
            return hour + 12;
        }
        if ("上午".equals(period) && hour == 12) {
            return 0;
        }
        return hour;
    }

    private static long durationMillis(int amount, String unit) {
        if (amount <= 0 || amount > 99) {
            return -1L;
        }
        if ("小时".equals(unit)) {
            return amount * 60L * 60L * 1000L;
        }
        return amount * 60L * 1000L;
    }

    private static long atRelativeDate(long referenceAt, int dayOffset, int hour, int minute) {
        Calendar calendar = calendar(referenceAt);
        calendar.add(Calendar.DAY_OF_YEAR, dayOffset);
        return setClock(calendar, hour, minute);
    }

    private static long atDate(long referenceAt, int year, int month, int day, int hour, int minute) {
        if (year < 1 || month < 1 || month > 12 || day < 1 || hour < 0 || hour > 23
                || minute < 0 || minute > 59) {
            return -1L;
        }
        Calendar calendar = calendar(referenceAt);
        calendar.clear();
        calendar.setLenient(false);
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        try {
            return calendar.getTimeInMillis();
        } catch (IllegalArgumentException ignored) {
            return -1L;
        }
    }

    private static long setClock(Calendar calendar, int hour, int minute) {
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            return -1L;
        }
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static Calendar calendar(long referenceAt) {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.setTimeInMillis(referenceAt);
        return calendar;
    }

    private static int parseNumber(String value) {
        if (value == null) {
            return -1;
        }
        String normalized = value.trim().replace("个", "").replace("分", "");
        if (normalized.length() == 0) {
            return -1;
        }
        try {
            int arabic = Integer.parseInt(normalized);
            return arabic >= 0 && arabic <= 99 ? arabic : -1;
        } catch (NumberFormatException ignored) {
            // Continue with Chinese numerals.
        }
        normalized = normalized.replace('兩', '两');
        int direct = chineseDigit(normalized);
        if (direct >= 0) {
            return direct;
        }
        int tenIndex = normalized.indexOf('十');
        if (tenIndex >= 0) {
            String tensText = normalized.substring(0, tenIndex);
            String onesText = normalized.substring(tenIndex + 1);
            int tens = tensText.length() == 0 ? 1 : chineseDigit(tensText);
            int ones = onesText.length() == 0 ? 0 : chineseDigit(onesText);
            if (tens >= 0 && tens <= 9 && ones >= 0 && ones <= 9) {
                return tens * 10 + ones;
            }
        }
        return -1;
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static int chineseDigit(String value) {
        if (value == null || value.length() != 1) {
            return -1;
        }
        switch (value.charAt(0)) {
            case '零':
            case '〇':
                return 0;
            case '一':
            case '壹':
                return 1;
            case '二':
            case '两':
            case '貳':
                return 2;
            case '三':
            case '叁':
                return 3;
            case '四':
                return 4;
            case '五':
                return 5;
            case '六':
                return 6;
            case '七':
                return 7;
            case '八':
                return 8;
            case '九':
                return 9;
            default:
                return -1;
        }
    }

    public static final class ParseResult {
        private final List<Candidate> candidates;
        private final long referenceAt;
        private final String sourceExpression;
        private final boolean suppressed;

        private ParseResult(
                List<Candidate> candidates,
                long referenceAt,
                String sourceExpression,
                boolean suppressed) {
            this.candidates = Collections.unmodifiableList(new ArrayList<>(candidates));
            this.referenceAt = referenceAt;
            this.sourceExpression = sourceExpression == null ? "" : sourceExpression;
            this.suppressed = suppressed;
        }

        static ParseResult empty(long referenceAt) {
            return new ParseResult(Collections.<Candidate>emptyList(), referenceAt, "", false);
        }

        static ParseResult suppressed(long referenceAt) {
            return new ParseResult(Collections.<Candidate>emptyList(), referenceAt, "", true);
        }

        public List<Candidate> getCandidates() {
            return candidates;
        }

        public Candidate getCandidate() {
            return candidates.size() == 1 ? candidates.get(0) : null;
        }

        public boolean isUnique() {
            return candidates.size() == 1;
        }

        public boolean isSuppressed() {
            return suppressed;
        }

        public boolean isAutoEligible() {
            return !suppressed && isUnique() && getCandidate().isFuture();
        }

        public long getReferenceAt() {
            return referenceAt;
        }

        public long getBaselineAt() {
            return referenceAt;
        }

        public String getSourceExpression() {
            return sourceExpression;
        }
    }

    public static final class Candidate {
        private final long triggerAt;
        private final String matchedText;
        private final long referenceAt;
        private final boolean future;

        private Candidate(long triggerAt, String matchedText, long referenceAt) {
            this.triggerAt = triggerAt;
            this.matchedText = matchedText == null ? "" : matchedText.trim();
            this.referenceAt = referenceAt;
            this.future = triggerAt > referenceAt;
        }

        public long getTriggerAt() {
            return triggerAt;
        }

        public String getMatchedText() {
            return matchedText;
        }

        public String getSourceExpression() {
            return matchedText;
        }

        public long getReferenceAt() {
            return referenceAt;
        }

        public long getBaselineAt() {
            return referenceAt;
        }

        public boolean isFuture() {
            return future;
        }

        public String getDisplayTime() {
            return TodoDateTime.format(triggerAt);
        }
    }

    private static final class RawMatch {
        private final int start;
        private final int end;
        private final Candidate candidate;

        private RawMatch(int start, int end, Candidate candidate) {
            this.start = start;
            this.end = end;
            this.candidate = candidate;
        }
    }

    private static final class Clock {
        private final int hour;
        private final int minute;

        private Clock(int hour, int minute) {
            this.hour = hour;
            this.minute = minute;
        }
    }
}

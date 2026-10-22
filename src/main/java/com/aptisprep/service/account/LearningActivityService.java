package com.aptisprep.service.account;

import com.aptisprep.repository.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearningActivityService {
    public record Day(LocalDate date, int count, int level, boolean future, String label) {
    }

    public record Week(List<Day> days) {
    }

    public record ActivityView(List<Week> weeks, int currentStreak, int longestStreak,
                               int activeDays, int totalActivities, LocalDate from, LocalDate to) {
    }

    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private enum Kind {LESSON, PRACTICE, FLASHCARD, EXAM}

    private static final class DailyCount {
        int lesson, practice, flashcard, exam;

        void add(Kind kind) {
            switch (kind) {
                case LESSON -> lesson++;
                case PRACTICE -> practice++;
                case FLASHCARD -> flashcard++;
                case EXAM -> exam++;
            }
        }

        int total() {
            return lesson + practice + flashcard + exam;
        }

        String detail() {
            List<String> parts = new ArrayList<>();
            if (lesson > 0) parts.add(lesson + " bài học");
            if (practice > 0) parts.add(practice + " bài luyện tập");
            if (flashcard > 0) parts.add(flashcard + " thẻ đã ôn");
            if (exam > 0) parts.add(exam + " đề đã nộp");
            return String.join(" · ", parts);
        }
    }

    private final AccountAccessService access;
    private final LessonProgressRepository lessons;
    private final LessonCheckAttemptRepository checks;
    private final UserFlashcardProgressRepository flashcards;
    private final ExamAttemptRepository exams;

    public LearningActivityService(AccountAccessService access, LessonProgressRepository lessons,
                                   LessonCheckAttemptRepository checks, UserFlashcardProgressRepository flashcards,
                                   ExamAttemptRepository exams) {
        this.access = access;
        this.lessons = lessons;
        this.checks = checks;
        this.flashcards = flashcards;
        this.exams = exams;
    }

    @Transactional(readOnly = true)
    public ActivityView activity(String email) {
        Integer userId = access.requireActiveStudent(email);
        Map<LocalDate, DailyCount> counts = new HashMap<>();
        lessons.findByUserId(userId).forEach(item -> {
            add(counts, item.getLastAccessedAt(), Kind.LESSON);
            if (item.getCompletedAt() != null && !sameDay(item.getCompletedAt(), item.getLastAccessedAt()))
                add(counts, item.getCompletedAt(), Kind.LESSON);
        });
        checks.findByUserId(userId).forEach(item -> add(counts, item.getSubmittedAt(), Kind.PRACTICE));
        flashcards.findByUserId(userId).forEach(item -> add(counts, item.getLastReviewedAt(), Kind.FLASHCARD));
        exams.findByUserId(userId).forEach(item -> add(counts, item.getSubmittedAt(), Kind.EXAM));
        return summarizeDetailed(counts, LocalDate.now());
    }

    static ActivityView summarize(Map<LocalDate, Integer> source, LocalDate today) {
        Map<LocalDate, DailyCount> detailed = new HashMap<>();
        source.forEach((date, count) -> {
            var value = new DailyCount();
            value.lesson = Math.max(0, count);
            detailed.put(date, value);
        });
        return summarizeDetailed(detailed, today);
    }

    private static ActivityView summarizeDetailed(Map<LocalDate, DailyCount> source, LocalDate today) {
        LocalDate start = today.minusWeeks(52).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        List<Week> weeks = new ArrayList<>();
        int total = 0, active = 0, longest = 0, running = 0;
        for (LocalDate date = start; !date.isAfter(today); date = date.plusDays(1)) {
            int count = source.getOrDefault(date, new DailyCount()).total();
            total += count;
            if (count > 0) {
                active++;
                running++;
                longest = Math.max(longest, running);
            } else running = 0;
        }
        LocalDate cursor = source.getOrDefault(today, new DailyCount()).total() > 0 ? today : today.minusDays(1);
        int current = 0;
        while (!cursor.isBefore(start) && source.getOrDefault(cursor, new DailyCount()).total() > 0) {
            current++;
            cursor = cursor.minusDays(1);
        }
        for (int week = 0; week < 53; week++) {
            List<Day> days = new ArrayList<>();
            for (int weekday = 0; weekday < 7; weekday++) {
                LocalDate date = start.plusDays((long) week * 7 + weekday);
                boolean future = date.isAfter(today);
                DailyCount detail = source.getOrDefault(date, new DailyCount());
                int count = future ? 0 : detail.total();
                days.add(new Day(date, count, level(count), future,
                        future ? "" : date.format(LABEL) + (count == 0 ? ": Không có hoạt động" : ": " + detail.detail())));
            }
            weeks.add(new Week(List.copyOf(days)));
        }
        return new ActivityView(List.copyOf(weeks), current, longest, active, total, start, today);
    }

    private static int level(int count) {
        return count <= 0 ? 0 : count == 1 ? 1 : count <= 3 ? 2 : count <= 6 ? 3 : 4;
    }

    private static void add(Map<LocalDate, DailyCount> counts, LocalDateTime value, Kind kind) {
        if (value != null) counts.computeIfAbsent(value.toLocalDate(), ignored -> new DailyCount()).add(kind);
    }

    private static boolean sameDay(LocalDateTime left, LocalDateTime right) {
        return left != null && right != null && left.toLocalDate().equals(right.toLocalDate());
    }
}

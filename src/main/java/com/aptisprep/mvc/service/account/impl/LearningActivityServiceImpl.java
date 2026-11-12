package com.aptisprep.mvc.service.account.impl;

import com.aptisprep.mvc.repository.*;
import com.aptisprep.mvc.service.account.AccountAccessService;
import com.aptisprep.mvc.service.account.LearningActivityService;
import com.aptisprep.mvc.service.account.LearningActivityService.ActivityView;
import com.aptisprep.mvc.service.account.LearningActivityService.Day;
import com.aptisprep.mvc.service.account.LearningActivityService.Week;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearningActivityServiceImpl implements LearningActivityService {
    private DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private enum Kind {
        LESSON,
        PRACTICE,
        FLASHCARD,
        EXAM
    }

    private class DailyCount {
        int lesson, practice, flashcard, exam;

        private void add(Kind kind) {
            switch (kind) {
                case LESSON -> lesson++;
                case PRACTICE -> practice++;
                case FLASHCARD -> flashcard++;
                case EXAM -> exam++;
            }
        }

        private int total() {
            return lesson + practice + flashcard + exam;
        }

        private String detail() {
            List<String> parts = new ArrayList<>();
            if (lesson > 0) parts.add(lesson + " bài học");
            if (practice > 0) parts.add(practice + " bài luyện tập");
            if (flashcard > 0) parts.add(flashcard + " thẻ đã ôn");
            if (exam > 0) parts.add(exam + " đề đã nộp");
            return String.join(" · ", parts);
        }
    }

    private AccountAccessService access;
    private LessonProgressRepository lessonProgressRepository;
    private LessonCheckAttemptRepository lessonCheckAttemptRepository;
    private UserFlashcardProgressRepository userFlashcardProgressRepository;
    private ExamAttemptRepository examAttemptRepository;

    public LearningActivityServiceImpl(
            AccountAccessService access,
            LessonProgressRepository lessonProgressRepository,
            LessonCheckAttemptRepository lessonCheckAttemptRepository,
            UserFlashcardProgressRepository userFlashcardProgressRepository,
            ExamAttemptRepository examAttemptRepository) {
        this.access = access;
        this.lessonProgressRepository = lessonProgressRepository;
        this.lessonCheckAttemptRepository = lessonCheckAttemptRepository;
        this.userFlashcardProgressRepository = userFlashcardProgressRepository;
        this.examAttemptRepository = examAttemptRepository;
    }

    @Transactional(readOnly = true)
    public ActivityView activity(String email) {
        Integer userId = access.requireActiveStudent(email);
        Map<LocalDate, DailyCount> counts = new HashMap<>();
        lessonProgressRepository
                .findByUserId(userId)
                .forEach(
                        item -> {
                            add(counts, item.getLastAccessedAt(), Kind.LESSON);
                            if (item.getCompletedAt() != null
                                    && !sameDay(item.getCompletedAt(), item.getLastAccessedAt()))
                                add(counts, item.getCompletedAt(), Kind.LESSON);
                        });
        lessonCheckAttemptRepository
                .findByUserId(userId)
                .forEach(item -> add(counts, item.getSubmittedAt(), Kind.PRACTICE));
        userFlashcardProgressRepository
                .findByUserId(userId)
                .forEach(item -> add(counts, item.getLastReviewedAt(), Kind.FLASHCARD));
        examAttemptRepository
                .findByUserId(userId)
                .forEach(item -> add(counts, item.getSubmittedAt(), Kind.EXAM));
        return summarizeDetailed(counts, LocalDate.now());
    }

    @Override
    public ActivityView summarize(Map<LocalDate, Integer> activityByDate, LocalDate today) {
        Map<LocalDate, DailyCount> counts = new HashMap<>();
        activityByDate.forEach(
                (date, count) -> {
                    DailyCount dailyCount = new DailyCount();
                    dailyCount.lesson = count == null ? 0 : count;
                    counts.put(date, dailyCount);
                });
        return summarizeDetailed(counts, today);
    }

    private ActivityView summarizeDetailed(Map<LocalDate, DailyCount> source, LocalDate today) {
        LocalDate start =
                today.minusWeeks(52).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
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
        LocalDate cursor =
                source.getOrDefault(today, new DailyCount()).total() > 0
                        ? today
                        : today.minusDays(1);
        int current = 0;
        while (!cursor.isBefore(start)
                && source.getOrDefault(cursor, new DailyCount()).total() > 0) {
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
                days.add(
                        new Day(
                                date,
                                count,
                                level(count),
                                future,
                                future
                                        ? ""
                                        : date.format(LABEL)
                                                + (count == 0
                                                        ? ": Không có hoạt động"
                                                        : ": " + detail.detail())));
            }
            weeks.add(new Week(List.copyOf(days)));
        }
        return new ActivityView(List.copyOf(weeks), current, longest, active, total, start, today);
    }

    private int level(int count) {
        return count <= 0 ? 0 : count == 1 ? 1 : count <= 3 ? 2 : count <= 6 ? 3 : 4;
    }

    private void add(Map<LocalDate, DailyCount> counts, LocalDateTime value, Kind kind) {
        if (value != null)
            counts.computeIfAbsent(value.toLocalDate(), ignored -> new DailyCount()).add(kind);
    }

    private boolean sameDay(LocalDateTime left, LocalDateTime right) {
        return left != null && right != null && left.toLocalDate().equals(right.toLocalDate());
    }
}

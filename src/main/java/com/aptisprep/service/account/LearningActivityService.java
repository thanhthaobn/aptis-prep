package com.aptisprep.service.account;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface LearningActivityService {
    record Day(LocalDate date, int count, int level, boolean future, String label) {}

    record Week(List<Day> days) {}

    record ActivityView(
            List<Week> weeks,
            int currentStreak,
            int longestStreak,
            int activeDays,
            int totalActivities,
            LocalDate from,
            LocalDate to) {}

    ActivityView activity(String email);

    static ActivityView summarize(Map<LocalDate, Integer> source, LocalDate today) {
        LocalDate start =
                today.minusWeeks(52).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        List<Week> weeks = new ArrayList<>();
        int total = 0, active = 0, longest = 0, running = 0;
        for (LocalDate date = start; !date.isAfter(today); date = date.plusDays(1)) {
            int count = Math.max(0, source.getOrDefault(date, 0));
            total += count;
            if (count > 0) {
                active++;
                running++;
                longest = Math.max(longest, running);
            } else running = 0;
        }
        LocalDate cursor = source.getOrDefault(today, 0) > 0 ? today : today.minusDays(1);
        int current = 0;
        while (!cursor.isBefore(start) && source.getOrDefault(cursor, 0) > 0) {
            current++;
            cursor = cursor.minusDays(1);
        }
        DateTimeFormatter label = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (int week = 0; week < 53; week++) {
            List<Day> days = new ArrayList<>();
            for (int weekday = 0; weekday < 7; weekday++) {
                LocalDate date = start.plusDays((long) week * 7 + weekday);
                boolean future = date.isAfter(today);
                int count = future ? 0 : Math.max(0, source.getOrDefault(date, 0));
                days.add(
                        new Day(
                                date,
                                count,
                                count <= 0
                                        ? 0
                                        : count == 1 ? 1 : count <= 3 ? 2 : count <= 6 ? 3 : 4,
                                future,
                                future ? "" : date.format(label)));
            }
            weeks.add(new Week(List.copyOf(days)));
        }
        return new ActivityView(List.copyOf(weeks), current, longest, active, total, start, today);
    }
}

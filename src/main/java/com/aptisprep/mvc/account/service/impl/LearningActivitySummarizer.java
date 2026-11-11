package com.aptisprep.mvc.account.service.impl;

import com.aptisprep.mvc.account.dto.LearningActivityDTO.ActivityViewDTO;
import com.aptisprep.mvc.account.dto.LearningActivityDTO.DayDTO;
import com.aptisprep.mvc.account.dto.LearningActivityDTO.WeekDTO;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class LearningActivitySummarizer {
    private LearningActivitySummarizer() {}

    static ActivityViewDTO summarize(Map<LocalDate, Integer> source, LocalDate today) {
        LocalDate start =
                today.minusWeeks(52).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        List<WeekDTO> weeks = new ArrayList<>();
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
            List<DayDTO> days = new ArrayList<>();
            for (int weekday = 0; weekday < 7; weekday++) {
                LocalDate date = start.plusDays((long) week * 7 + weekday);
                boolean future = date.isAfter(today);
                int count = future ? 0 : Math.max(0, source.getOrDefault(date, 0));
                days.add(
                        new DayDTO(
                                date,
                                count,
                                count <= 0
                                        ? 0
                                        : count == 1 ? 1 : count <= 3 ? 2 : count <= 6 ? 3 : 4,
                                future,
                                future ? "" : date.format(label)));
            }
            weeks.add(new WeekDTO(List.copyOf(days)));
        }
        return new ActivityViewDTO(
                List.copyOf(weeks), current, longest, active, total, start, today);
    }
}

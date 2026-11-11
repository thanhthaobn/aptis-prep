package com.aptisprep.mvc.account.dto;

import java.time.LocalDate;
import java.util.List;

public final class LearningActivityDTO {
    private LearningActivityDTO() {}

    public record DayDTO(LocalDate date, int count, int level, boolean future, String label) {}

    public record WeekDTO(List<DayDTO> days) {}

    public record ActivityViewDTO(
            List<WeekDTO> weeks,
            int currentStreak,
            int longestStreak,
            int activeDays,
            int totalActivities,
            LocalDate from,
            LocalDate to) {}
}

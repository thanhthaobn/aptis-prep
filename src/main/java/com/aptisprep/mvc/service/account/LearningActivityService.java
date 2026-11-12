package com.aptisprep.mvc.service.account;

import java.time.LocalDate;
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

    public ActivityView activity(String email);

    public ActivityView summarize(Map<LocalDate, Integer> activityByDate, LocalDate today);
}

package com.aptisprep.dto;

import java.time.LocalDate;
import java.util.List;

public final class LearningActivityDTO {
    private LearningActivityDTO() {}

    public static class DayDTO {
        private LocalDate date;
        private int count;
        private int level;
        private boolean future;
        private String label;

        public DayDTO() {}

        public DayDTO(LocalDate date, int count, int level, boolean future, String label) {
            this.date = date;
            this.count = count;
            this.level = level;
            this.future = future;
            this.label = label;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public LocalDate date() {
            return date;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int count() {
            return count;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public int level() {
            return level;
        }

        public boolean isFuture() {
            return future;
        }

        public void setFuture(boolean future) {
            this.future = future;
        }

        public boolean future() {
            return future;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public static class WeekDTO {
        private List<DayDTO> days;

        public WeekDTO() {}

        public WeekDTO(List<DayDTO> days) {
            this.days = days;
        }

        public List<DayDTO> getDays() {
            return days;
        }

        public void setDays(List<DayDTO> days) {
            this.days = days;
        }

        public List<DayDTO> days() {
            return days;
        }
    }

    public static class ActivityViewDTO {
        private List<WeekDTO> weeks;
        private int currentStreak;
        private int longestStreak;
        private int activeDays;
        private int totalActivities;
        private LocalDate from;
        private LocalDate to;

        public ActivityViewDTO() {}

        public ActivityViewDTO(
                List<WeekDTO> weeks,
                int currentStreak,
                int longestStreak,
                int activeDays,
                int totalActivities,
                LocalDate from,
                LocalDate to) {
            this.weeks = weeks;
            this.currentStreak = currentStreak;
            this.longestStreak = longestStreak;
            this.activeDays = activeDays;
            this.totalActivities = totalActivities;
            this.from = from;
            this.to = to;
        }

        public List<WeekDTO> getWeeks() {
            return weeks;
        }

        public void setWeeks(List<WeekDTO> weeks) {
            this.weeks = weeks;
        }

        public List<WeekDTO> weeks() {
            return weeks;
        }

        public int getCurrentStreak() {
            return currentStreak;
        }

        public void setCurrentStreak(int currentStreak) {
            this.currentStreak = currentStreak;
        }

        public int currentStreak() {
            return currentStreak;
        }

        public int getLongestStreak() {
            return longestStreak;
        }

        public void setLongestStreak(int longestStreak) {
            this.longestStreak = longestStreak;
        }

        public int longestStreak() {
            return longestStreak;
        }

        public int getActiveDays() {
            return activeDays;
        }

        public void setActiveDays(int activeDays) {
            this.activeDays = activeDays;
        }

        public int activeDays() {
            return activeDays;
        }

        public int getTotalActivities() {
            return totalActivities;
        }

        public void setTotalActivities(int totalActivities) {
            this.totalActivities = totalActivities;
        }

        public int totalActivities() {
            return totalActivities;
        }

        public LocalDate getFrom() {
            return from;
        }

        public void setFrom(LocalDate from) {
            this.from = from;
        }

        public LocalDate from() {
            return from;
        }

        public LocalDate getTo() {
            return to;
        }

        public void setTo(LocalDate to) {
            this.to = to;
        }

        public LocalDate to() {
            return to;
        }
    }
}

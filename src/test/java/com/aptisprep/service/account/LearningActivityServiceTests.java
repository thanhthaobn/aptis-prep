package com.aptisprep.service.account;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LearningActivityServiceTests {
    @Test
    void calculatesCurrentAndLongestStreakFromRealActivityDates() {
        LocalDate today = LocalDate.of(2026, 10, 21);
        var result =
                LearningActivityService.summarize(
                        Map.of(
                                today,
                                2,
                                today.minusDays(1),
                                1,
                                today.minusDays(2),
                                4,
                                today.minusDays(5),
                                1,
                                today.minusDays(6),
                                1,
                                today.minusDays(7),
                                1,
                                today.minusDays(8),
                                1),
                        today);
        assertThat(result.currentStreak()).isEqualTo(3);
        assertThat(result.longestStreak()).isEqualTo(4);
        assertThat(result.activeDays()).isEqualTo(7);
        assertThat(result.totalActivities()).isEqualTo(11);
        assertThat(result.weeks()).hasSize(53);
        assertThat(result.weeks()).allSatisfy(week -> assertThat(week.days()).hasSize(7));
    }

    @Test
    void keepsYesterdayStreakCurrentUntilLearnerStudiesToday() {
        LocalDate today = LocalDate.of(2026, 10, 21);
        var result =
                LearningActivityService.summarize(
                        Map.of(
                                today.minusDays(1), 2,
                                today.minusDays(2), 1),
                        today);
        assertThat(result.currentStreak()).isEqualTo(2);
    }
}

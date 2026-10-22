package com.aptisprep.dto.view;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AccountSnapshot(String fullName, String email, GoalView goal, List<SkillView> profiles) {
    public record GoalView(Integer certificateId, String certificateName, LocalDate targetDate,
                           Map<String, String> targets) {
    }

    public record SkillView(String skill, String currentLevel, Integer sourceAttemptId) {
    }
}

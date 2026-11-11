package com.aptisprep.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AccountSnapshotDTO(
        String fullName, String email, GoalViewDTO goal, List<SkillViewDTO> profiles) {
    public record GoalViewDTO(
            Integer certificateId,
            String certificateName,
            LocalDate targetDate,
            Map<String, String> targets,
            Map<String, BigDecimal> targetScores) {
        public GoalViewDTO(
                Integer certificateId,
                String certificateName,
                LocalDate targetDate,
                Map<String, String> targets) {
            this(certificateId, certificateName, targetDate, targets, Map.of());
        }
    }

    public record SkillViewDTO(String skill, String currentLevel, Integer sourceAttemptId) {}
}

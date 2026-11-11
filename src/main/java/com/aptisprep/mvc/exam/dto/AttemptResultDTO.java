package com.aptisprep.mvc.exam.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AttemptResultDTO(
        Integer attemptId,
        Integer userId,
        Integer examId,
        String examType,
        String examTitle,
        Integer scoringSchemeId,
        String state,
        List<SkillResultDTO> skills,
        BigDecimal finalScaleScore,
        String overallCefr,
        Instant submittedAt) {
    public boolean canDiscard() {
        return "ERROR".equals(state)
                || "EXPIRED".equals(state)
                || "COMPLETED".equals(state)
                || ("IN_PROGRESS".equals(state)
                        && submittedAt == null
                        && finalScaleScore == null
                        && overallCefr == null
                        && (skills == null || skills.isEmpty()));
    }

    public boolean hasFullMockTotal() {
        return isCombined() && finalScaleScore != null;
    }

    public boolean isCombined() {
        return "FULL_MOCK".equals(examType)
                || ("PLACEMENT".equals(examType)
                        && skills != null
                        && skills.stream()
                                .map(SkillResultDTO::skill)
                                .collect(java.util.stream.Collectors.toSet())
                                .containsAll(com.aptisprep.mvc.common.enums.Skill.codes()));
    }

    public String stateLabel() {
        return switch (state == null ? "" : state) {
            case "COMPLETED" -> "Đã hoàn thành";
            case "ERROR" -> "Chấm điểm gặp lỗi";
            case "EXPIRED" -> "Đã hết thời gian";
            case "IN_PROGRESS" -> "Đang làm bài";
            default -> "Đang chấm điểm";
        };
    }

    public String examTypeLabel() {
        return switch (examType == null ? "" : examType) {
            case "PLACEMENT" -> "Kiểm tra đầu vào";
            case "CHECKPOINT" -> "Kiểm tra cuối chặng";
            case "FULL_MOCK" -> "Thi thử tổng hợp";
            default -> "Bài luyện tập";
        };
    }

    public String stateBadgeClass() {
        return switch (state == null ? "" : state) {
            case "COMPLETED" -> "badge-success";
            case "ERROR" -> "badge-danger";
            case "EXPIRED" -> "badge-history-expired";
            case "IN_PROGRESS" -> "badge-info";
            default -> "badge-warning";
        };
    }

    public String stateClass() {
        return switch (state == null ? "" : state) {
            case "COMPLETED" -> " is-complete";
            case "ERROR" -> " is-error";
            default -> " is-pending";
        };
    }

    public boolean hasPendingGrades() {
        return skills != null
                && skills.stream()
                        .anyMatch(
                                skill ->
                                        "PENDING".equals(skill.gradingState())
                                                || "WAITING".equals(skill.gradingState()));
    }

    public boolean hasGradingErrors() {
        return skills != null
                && skills.stream().anyMatch(skill -> "ERROR".equals(skill.gradingState()));
    }

    public boolean hasRetryableGrades() {
        return skills != null && skills.stream().anyMatch(SkillResultDTO::retryable);
    }

    public AttemptResultDTO(
            Integer attemptId,
            Integer userId,
            Integer examId,
            String examType,
            Integer scoringSchemeId,
            String state,
            List<SkillResultDTO> skills) {
        this(
                attemptId,
                userId,
                examId,
                examType,
                null,
                scoringSchemeId,
                state,
                skills,
                null,
                null,
                null);
    }

    public AttemptResultDTO(
            Integer attemptId,
            Integer userId,
            Integer examId,
            String examType,
            Integer scoringSchemeId,
            String state,
            List<SkillResultDTO> skills,
            Instant submittedAt) {
        this(
                attemptId,
                userId,
                examId,
                examType,
                null,
                scoringSchemeId,
                state,
                skills,
                null,
                null,
                submittedAt);
    }

    public AttemptResultDTO(
            Integer attemptId,
            Integer userId,
            Integer examId,
            String examType,
            Integer scoringSchemeId,
            String state,
            List<SkillResultDTO> skills,
            Instant submittedAt,
            BigDecimal overallScaleScore,
            String overallCefr) {
        this(
                attemptId,
                userId,
                examId,
                examType,
                null,
                scoringSchemeId,
                state,
                skills,
                overallScaleScore,
                overallCefr,
                submittedAt);
    }

    public BigDecimal overallScaleScore() {
        return finalScaleScore;
    }
}

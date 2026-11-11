package com.aptisprep.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class AttemptResultDTO {
    private Integer attemptId;
    private Integer userId;
    private Integer examId;
    private String examType;
    private String examTitle;
    private Integer scoringSchemeId;
    private String state;
    private List<SkillResultDTO> skills;
    private BigDecimal finalScaleScore;
    private String overallCefr;
    private Instant submittedAt;

    public AttemptResultDTO() {}

    public AttemptResultDTO(
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
        this.attemptId = attemptId;
        this.userId = userId;
        this.examId = examId;
        this.examType = examType;
        this.examTitle = examTitle;
        this.scoringSchemeId = scoringSchemeId;
        this.state = state;
        this.skills = skills;
        this.finalScaleScore = finalScaleScore;
        this.overallCefr = overallCefr;
        this.submittedAt = submittedAt;
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
                                .containsAll(com.aptisprep.enums.Skill.codes()));
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

    public BigDecimal overallScaleScore() {
        return finalScaleScore;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public Integer attemptId() {
        return attemptId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer userId() {
        return userId;
    }

    public Integer getExamId() {
        return examId;
    }

    public void setExamId(Integer examId) {
        this.examId = examId;
    }

    public Integer examId() {
        return examId;
    }

    public String getExamType() {
        return examType;
    }

    public void setExamType(String examType) {
        this.examType = examType;
    }

    public String examType() {
        return examType;
    }

    public String getExamTitle() {
        return examTitle;
    }

    public void setExamTitle(String examTitle) {
        this.examTitle = examTitle;
    }

    public String examTitle() {
        return examTitle;
    }

    public Integer getScoringSchemeId() {
        return scoringSchemeId;
    }

    public void setScoringSchemeId(Integer scoringSchemeId) {
        this.scoringSchemeId = scoringSchemeId;
    }

    public Integer scoringSchemeId() {
        return scoringSchemeId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String state() {
        return state;
    }

    public List<SkillResultDTO> getSkills() {
        return skills;
    }

    public void setSkills(List<SkillResultDTO> skills) {
        this.skills = skills;
    }

    public List<SkillResultDTO> skills() {
        return skills;
    }

    public BigDecimal getFinalScaleScore() {
        return finalScaleScore;
    }

    public void setFinalScaleScore(BigDecimal finalScaleScore) {
        this.finalScaleScore = finalScaleScore;
    }

    public BigDecimal finalScaleScore() {
        return finalScaleScore;
    }

    public String getOverallCefr() {
        return overallCefr;
    }

    public void setOverallCefr(String overallCefr) {
        this.overallCefr = overallCefr;
    }

    public String overallCefr() {
        return overallCefr;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant submittedAt() {
        return submittedAt;
    }
}

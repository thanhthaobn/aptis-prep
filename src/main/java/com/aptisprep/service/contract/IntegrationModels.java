package com.aptisprep.service.contract;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Integration DTOs only. Not entities, Spring beans or persistence enum contracts. */
public final class IntegrationModels {
    private IntegrationModels() {}
    public record SkillTarget(String skill, String targetLevel) {}
    public record SkillLevel(String skill, String estimatedLevel, Integer sourceAttemptId) {}
    public record LearnerContext(Integer userId, List<SkillTarget> targets, List<SkillLevel> levels) {}
    public record PathProgress(Integer pathId, String skill, String pathSource, String pathStatus,
            int completedLessons, int totalLessons, Integer progressPercent,
            int completedRequiredLessons, int totalRequiredLessons,
            Integer nextLessonId, String nextLessonTitle, String checkpointState) {}
    public record ExamSummary(Integer examId, String title, String examType, String skill,
            String targetLevel, Integer durationMinutes, Integer scoringSchemeId) {}
    /** Safe learner view: deliberately contains no answers or internal grading context. */
    public record QuestionView(Integer questionId, String skill, String type, int position, String content) {}
    public record ExamView(ExamSummary exam, List<QuestionView> questions) {}
    public record AttemptHandle(Integer attemptId, String state, Instant deadline) {}
    public record SkillResult(String skill, BigDecimal rawScore, BigDecimal scaleScore,
            String estimatedLevel, String gradingState, String feedback) {}
    public record AttemptResult(Integer attemptId, Integer userId, Integer examId, String examType,
            Integer scoringSchemeId, String state, List<SkillResult> skills) {}
    public record HistoryPage(List<AttemptResult> items, int page, int size, long totalItems) {}
}

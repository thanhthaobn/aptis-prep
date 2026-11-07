package com.aptisprep.service.contract;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Integration DTOs only. Not entities, Spring beans or persistence enum contracts. */
public final class IntegrationModels {
    private IntegrationModels() {}

    public record SkillTarget(String skill, String targetLevel) {}

    public record SkillLevel(String skill, String estimatedLevel, Integer sourceAttemptId) {}

    public record LearnerContext(
            Integer userId, List<SkillTarget> targets, List<SkillLevel> levels) {}

    public record PathProgress(
            Integer pathId,
            String skill,
            String pathSource,
            String pathStatus,
            int completedLessons,
            int totalLessons,
            Integer progressPercent,
            int completedRequiredLessons,
            int totalRequiredLessons,
            Integer nextLessonId,
            String nextLessonTitle,
            String checkpointState) {}

    public record ExamSummary(
            Integer examId,
            String title,
            String examType,
            String skill,
            String targetLevel,
            Integer durationMinutes,
            Integer scoringSchemeId) {}

    /** Safe learner view: deliberately contains no answers or internal grading context. */
    public record QuestionView(
            Integer questionId, String skill, String type, int position, String content) {}

    public record ExamView(ExamSummary exam, List<QuestionView> questions) {}

    public record AttemptHandle(Integer attemptId, String state, Instant deadline) {}

    /** Backend-only snapshot emitted after the attempt is locked. Never bind this from a form. */
    public record LockedAttempt(
            Integer attemptId,
            Integer userId,
            Integer examId,
            String examType,
            Integer answerVersion,
            Instant submittedAt,
            Integer scoringSchemeId,
            String policyVersion,
            String rubricVersion,
            String gradingContext,
            String gradingRequestKey,
            List<LockedAnswer> answers) {
        public LockedAttempt(
                Integer attemptId,
                Integer userId,
                Integer examId,
                String examType,
                Integer answerVersion,
                Instant submittedAt,
                Integer scoringSchemeId,
                String policyVersion,
                String gradingRequestKey,
                List<LockedAnswer> answers) {
            this(
                    attemptId,
                    userId,
                    examId,
                    examType,
                    answerVersion,
                    submittedAt,
                    scoringSchemeId,
                    policyVersion,
                    "aptis-rubric-v1",
                    null,
                    gradingRequestKey,
                    answers);
        }
    }

    public record LockedAnswer(
            Integer answerId,
            Integer questionId,
            String skill,
            String questionType,
            String taskContent,
            String lockedContent,
            String transcript,
            List<LockedItem> items) {
        public LockedAnswer(
                Integer answerId,
                Integer questionId,
                String skill,
                String questionType,
                String taskContent,
                String lockedContent,
                String transcript) {
            this(
                    answerId,
                    questionId,
                    skill,
                    questionType,
                    taskContent,
                    lockedContent,
                    transcript,
                    List.of());
        }
    }

    /** Backend-only item snapshot. Never serialize this record to a learner-facing view. */
    public record LockedItem(
            Integer itemId,
            int position,
            String taskContent,
            String lockedContent,
            Integer correctOptionId,
            Integer correctPosition) {
        public LockedItem(Integer itemId, Integer correctOptionId, Integer correctPosition) {
            this(itemId, 0, null, null, correctOptionId, correctPosition);
        }
    }

    public record SkillResult(
            String skill,
            BigDecimal rawScore,
            BigDecimal scaleScore,
            String estimatedLevel,
            String gradingState,
            String feedback,
            boolean passed,
            boolean retryable) {
        public SkillResult(
                String skill,
                BigDecimal rawScore,
                BigDecimal scaleScore,
                String estimatedLevel,
                String gradingState,
                String feedback) {
            this(skill, rawScore, scaleScore, estimatedLevel, gradingState, feedback, false, false);
        }

        public SkillResult(
                String skill,
                BigDecimal rawScore,
                BigDecimal scaleScore,
                String estimatedLevel,
                String gradingState,
                String feedback,
                boolean passed) {
            this(
                    skill,
                    rawScore,
                    scaleScore,
                    estimatedLevel,
                    gradingState,
                    feedback,
                    passed,
                    false);
        }
    }

    public record AttemptResult(
            Integer attemptId,
            Integer userId,
            Integer examId,
            String examType,
            String examTitle,
            Integer scoringSchemeId,
            String state,
            List<SkillResult> skills,
            BigDecimal finalScaleScore,
            String overallCefr,
            Instant submittedAt) {
        public boolean hasFullMockTotal() {
            return "FULL_MOCK".equals(examType)
                    && finalScaleScore != null
                    && skills.stream()
                            .anyMatch(
                                    skill ->
                                            "GRAMMAR_VOCABULARY".equals(skill.skill())
                                                    && skill.scaleScore() != null
                                                    && skill.estimatedLevel() == null);
        }

        public AttemptResult(
                Integer attemptId,
                Integer userId,
                Integer examId,
                String examType,
                Integer scoringSchemeId,
                String state,
                List<SkillResult> skills) {
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

        public AttemptResult(
                Integer attemptId,
                Integer userId,
                Integer examId,
                String examType,
                Integer scoringSchemeId,
                String state,
                List<SkillResult> skills,
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

        public AttemptResult(
                Integer attemptId,
                Integer userId,
                Integer examId,
                String examType,
                Integer scoringSchemeId,
                String state,
                List<SkillResult> skills,
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

    public record HistoryFilter(String query, String skill, String examType, String state) {
        public static HistoryFilter empty() {
            return new HistoryFilter("", "", "", "");
        }
    }

    public record HistoryPage(List<AttemptResult> items, int page, int size, long totalItems) {}

    public record AdminHistoryFilter(
            String query,
            String skill,
            String examType,
            String state,
            Instant submittedFrom,
            Instant submittedTo) {}

    public record AdminHistoryItem(String learnerName, String learnerEmail, AttemptResult result) {}

    public record AdminHistoryPage(
            List<AdminHistoryItem> items, int page, int size, long totalItems) {}
}

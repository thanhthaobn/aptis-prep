package com.aptisprep.dto.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.core.io.Resource;

public final class ExamDTO {
    private ExamDTO() {}

    public record AttemptHandleDTO(Integer attemptId, String state, Instant deadline) {}

    public record LockedAttemptDTO(
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
            List<LockedAnswerDTO> answers) {
        public LockedAttemptDTO(
                Integer attemptId,
                Integer userId,
                Integer examId,
                String examType,
                Integer answerVersion,
                Instant submittedAt,
                Integer scoringSchemeId,
                String policyVersion,
                String gradingRequestKey,
                List<LockedAnswerDTO> answers) {
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

    public record LockedAnswerDTO(
            Integer answerId,
            Integer questionId,
            String skill,
            String questionType,
            String taskContent,
            String lockedContent,
            String transcript,
            List<LockedItemDTO> items) {
        public LockedAnswerDTO(
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

    public record LockedItemDTO(
            Integer itemId,
            int position,
            String taskContent,
            String lockedContent,
            Integer correctOptionId,
            Integer correctPosition) {
        public LockedItemDTO(Integer itemId, Integer correctOptionId, Integer correctPosition) {
            this(itemId, 0, null, null, correctOptionId, correctPosition);
        }
    }

    public record SkillResultDTO(
            String skill,
            BigDecimal rawScore,
            BigDecimal scaleScore,
            String estimatedLevel,
            String gradingState,
            String feedback,
            boolean passed,
            boolean retryable) {
        public SkillResultDTO(
                String skill,
                BigDecimal rawScore,
                BigDecimal scaleScore,
                String estimatedLevel,
                String gradingState,
                String feedback) {
            this(skill, rawScore, scaleScore, estimatedLevel, gradingState, feedback, false, false);
        }

        public SkillResultDTO(
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

    public record HistoryFilterDTO(String query, String skill, String examType, String state) {
        public static HistoryFilterDTO empty() {
            return new HistoryFilterDTO("", "", "", "");
        }
    }

    public record HistoryPageDTO(
            List<AttemptResultDTO> items, int page, int size, long totalItems) {}

    public record AdminHistoryFilterDTO(
            String query,
            String skill,
            String examType,
            String state,
            Instant submittedFrom,
            Instant submittedTo) {}

    public record AdminHistoryItemDTO(
            String learnerName, String learnerEmail, AttemptResultDTO result) {}

    public record AdminHistoryPageDTO(
            List<AdminHistoryItemDTO> items, int page, int size, long totalItems) {}

    public record SectionPageDTO(
            int sequence,
            String skill,
            int durationMinutes,
            int endMinute,
            boolean current,
            boolean completed,
            boolean future) {}

    public record AttemptPageDTO(
            Integer attemptId,
            String title,
            String examType,
            String currentSectionSkill,
            Integer sectionDurationMinutes,
            int remainingSeconds,
            List<SectionPageDTO> sections,
            List<QuestionPageDTO> questions) {
        public boolean nextSection() {
            return sections != null && sections.size() > 1;
        }

        public AttemptPageDTO(
                Integer attemptId,
                String title,
                String examType,
                int remainingSeconds,
                List<QuestionPageDTO> questions) {
            this(attemptId, title, examType, null, null, remainingSeconds, List.of(), questions);
        }

        public AttemptPageDTO(
                Integer attemptId,
                String title,
                String examType,
                String currentSectionSkill,
                Integer sectionDurationMinutes,
                int remainingSeconds,
                List<QuestionPageDTO> questions) {
            this(
                    attemptId,
                    title,
                    examType,
                    currentSectionSkill,
                    sectionDurationMinutes,
                    remainingSeconds,
                    List.of(),
                    questions);
        }

        public AttemptPageDTO(
                Integer attemptId,
                String title,
                String examType,
                int remainingSeconds,
                List<SectionPageDTO> sections,
                List<QuestionPageDTO> questions) {
            this(
                    attemptId,
                    title,
                    examType,
                    sections.isEmpty() ? null : sections.getFirst().skill(),
                    sections.isEmpty() ? null : sections.getFirst().durationMinutes(),
                    remainingSeconds,
                    sections,
                    questions);
        }

        public int totalItems() {
            return questions == null
                    ? 0
                    : questions.stream()
                            .mapToInt(q -> q.items() == null ? 0 : q.items().size())
                            .sum();
        }

        public int answeredItems() {
            return questions == null
                    ? 0
                    : questions.stream()
                            .mapToInt(
                                    q ->
                                            q.items() == null
                                                    ? 0
                                                    : (int)
                                                            q.items().stream()
                                                                    .filter(ItemPageDTO::answered)
                                                                    .count())
                            .sum();
        }
    }

    public record QuestionPageDTO(
            Integer id,
            int position,
            String skill,
            String type,
            String typeLabel,
            String content,
            String instruction,
            String readingPassage,
            List<ItemPageDTO> items,
            List<MediaPageDTO> media) {}

    public record ItemPageDTO(
            Integer id,
            int position,
            String content,
            Integer responseSeconds,
            Integer minWordCount,
            Integer maxWordCount,
            Integer selectedOptionId,
            Integer userPosition,
            String textAnswer,
            String audioPath,
            String gradingStatus,
            List<OptionPageDTO> options) {
        public boolean answered() {
            return (selectedOptionId != null)
                    || (userPosition != null)
                    || (textAnswer != null && !textAnswer.trim().isEmpty())
                    || (audioPath != null && !audioPath.trim().isEmpty());
        }
    }

    public record OptionPageDTO(Integer id, String code, String content, boolean selected) {}

    public record MediaPageDTO(Integer id, String type, String filename, String contentType) {}

    public record MediaPlayResultDTO(
            boolean allowed, Integer remainingPlays, String audioUrl, String message) {}

    public record MediaResourceDTO(Resource resource, String contentType) {}

    public record AttemptReviewDTO(String title, List<QuestionPageDTO> questions) {}
}

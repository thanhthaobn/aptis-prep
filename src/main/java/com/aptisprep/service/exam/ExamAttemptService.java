package com.aptisprep.service.exam;

import com.aptisprep.dto.AdminHistoryFilterDTO;
import com.aptisprep.dto.AdminHistoryPageDTO;
import com.aptisprep.dto.AttemptHandleDTO;
import com.aptisprep.dto.AttemptResultDTO;
import com.aptisprep.dto.HistoryFilterDTO;
import com.aptisprep.dto.HistoryPageDTO;
import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.entity.MockExamQuestionEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface ExamAttemptService {
    public boolean hasInProgressPlacement(Integer userId, String skill);

    public boolean hasInProgressPlacement(Integer userId);

    public boolean hasPlacementAttempt(Integer userId);

    public AttemptHandleDTO start(Integer userId, Integer examId, String requestKey);

    public Integer latestResumableAttemptId(Integer userId, Integer examId);

    public AttemptHandleDTO startPlacement(Integer userId, Integer examId, String requestKey);

    public AttemptHandleDTO startCheckpoint(
            Integer userId, Integer examId, Integer pathId, String requestKey);

    public Optional<Integer> checkpointOriginPathId(Integer userId, Integer attemptId);

    public void submit(Integer userId, Integer attemptId, String requestKey);

    public AttemptResultDTO result(Integer userId, Integer attemptId);

    public HistoryPageDTO history(Integer userId, int page, int size);

    public HistoryPageDTO history(Integer userId, int page, int size, HistoryFilterDTO filter);

    public AdminHistoryPageDTO adminHistory(
            String adminEmail, int page, int size, AdminHistoryFilterDTO filter);

    public AttemptResultDTO adminResult(String adminEmail, Integer attemptId);

    public void retryFailedGrading(Integer userId, Integer attemptId, String requestKey);

    public boolean canRegradeEmpty(Integer userId, Integer attemptId);

    public void regradeEmpty(Integer userId, Integer attemptId);

    public void saveSpeakingAudio(
            Integer userId,
            Integer attemptId,
            Integer questionId,
            Integer itemId,
            MultipartFile audioFile);

    public MediaPlayResult playMedia(Integer userId, Integer attemptId, Integer mediaId);

    public MediaResource mediaContent(
            Integer userId, Integer attemptId, Integer mediaId, String token);

    public MediaResource speakingAudio(Integer userId, Integer attemptId, Integer answerId);

    public void saveAnswer(
            Integer userId,
            Integer attemptId,
            Integer questionId,
            Integer itemId,
            Integer selectedOptionId,
            Integer userPosition,
            String textAnswer);

    public void discard(Integer userId, Integer attemptId);

    public void advanceSection(Integer userId, Integer attemptId, int sectionEndMinute);

    public boolean sectionDeadlineReached(Integer userId, Integer attemptId, int sectionEndMinute);

    public boolean examDeadlineReached(Integer userId, Integer attemptId);

    public AttemptReview review(Integer userId, Integer attemptId);

    public AttemptReview adminReview(String adminEmail, Integer attemptId);

    public void adminDiscard(String adminEmail, Integer attemptId);

    public MediaResource reviewMedia(Integer userId, Integer attemptId, Integer mediaId);

    public MediaResource reviewRecording(
            Integer userId, Integer attemptId, Integer questionId, Integer itemId);

    public AttemptPage page(Integer userId, Integer attemptId);

    public void preserveLegacyAttempts(Integer examId);

    public void validateExamReady(MockExamEntity exam, List<MockExamQuestionEntity> links);

    public void validateExamForPublication(MockExamEntity exam, List<MockExamQuestionEntity> links);

    public List<java.time.LocalDateTime> findSubmittedDatesByUserId(Integer userId);

    public record SnapshotSchemeInfo(Integer scoringSchemeId, String skill) {}

    public Optional<SnapshotSchemeInfo> findSnapshotSchemeInfo(Integer attemptId);

    public boolean hasAnswersForQuestion(Integer questionId);

    public boolean hasAttemptsForScoringScheme(Integer schemeId);

    public record SectionPage(
            int index,
            String skill,
            int durationMinutes,
            int questionCount,
            String state,
            boolean open) {}

    public record AttemptPage(
            Integer attemptId,
            String title,
            String examType,
            Long remainingSeconds,
            List<QuestionPage> questions,
            Instant deadline,
            Integer nextSectionAfterMinute,
            String sectionSkill,
            List<SectionPage> sections) {
        public boolean nextSection() {
            return nextSectionAfterMinute != null;
        }

        public AttemptPage(
                Integer attemptId,
                String title,
                String examType,
                Long remainingSeconds,
                List<QuestionPage> questions,
                Instant deadline) {
            this(
                    attemptId,
                    title,
                    examType,
                    remainingSeconds,
                    questions,
                    deadline,
                    null,
                    null,
                    List.of());
        }

        public AttemptPage(
                Integer attemptId,
                String title,
                Long remainingSeconds,
                List<QuestionPage> questions,
                Instant deadline) {
            this(attemptId, title, null, remainingSeconds, questions, deadline);
        }

        public AttemptPage(
                Integer attemptId,
                String title,
                Long remainingSeconds,
                List<QuestionPage> questions) {
            this(attemptId, title, null, remainingSeconds, questions, null);
        }

        public int totalItems() {
            return questions == null
                    ? 0
                    : questions.stream()
                            .mapToInt(
                                    question ->
                                            question.items() == null ? 0 : question.items().size())
                            .sum();
        }

        public int answeredItems() {
            return questions == null
                    ? 0
                    : questions.stream()
                            .flatMap(
                                    question ->
                                            question.items() == null
                                                    ? Stream.empty()
                                                    : question.items().stream())
                            .filter(ItemPage::answered)
                            .mapToInt(item -> 1)
                            .sum();
        }
    }

    public record QuestionPage(
            Integer questionId,
            String content,
            String instruction,
            String readingPassage,
            String skill,
            String type,
            int position,
            List<ItemPage> items,
            List<MediaPage> media) {}

    public record ItemPage(
            Integer itemId,
            int position,
            String content,
            Integer responseSeconds,
            List<OptionPage> options,
            Integer selectedOptionId,
            Integer userPosition,
            @JsonIgnore Integer correctPosition,
            String textAnswer,
            boolean audioSaved,
            Integer minWordCount,
            Integer maxWordCount) {
        public boolean answered() {
            return selectedOptionId != null
                    || userPosition != null
                    || (textAnswer != null && !textAnswer.isBlank())
                    || audioSaved;
        }
    }

    public record OptionPage(
            Integer id, String code, String content, @JsonIgnore boolean correct) {}

    public record MediaPage(Integer id, String type, String filename, String contentType) {}

    public record MediaPlayResult(
            Integer mediaId,
            int playCount,
            int maxPlayCount,
            String contentToken,
            boolean browserSpeech,
            String speechText) {}

    public record MediaResource(Resource resource, String contentType) {}

    public record AttemptReview(String title, List<QuestionPage> questions) {}
}

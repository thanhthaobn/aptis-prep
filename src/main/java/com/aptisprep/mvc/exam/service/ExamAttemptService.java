package com.aptisprep.mvc.exam.service;

import com.aptisprep.mvc.catalog.entity.MockExamEntity;
import com.aptisprep.mvc.catalog.entity.MockExamQuestionEntity;
import com.aptisprep.service.contract.IntegrationModels.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface ExamAttemptService {
    boolean hasInProgressPlacement(Integer userId, String skill);

    boolean hasInProgressPlacement(Integer userId);

    boolean hasPlacementAttempt(Integer userId);

    AttemptHandle start(Integer userId, Integer examId, String requestKey);

    Integer latestResumableAttemptId(Integer userId, Integer examId);

    AttemptHandle startPlacement(Integer userId, Integer examId, String requestKey);

    AttemptHandle startCheckpoint(
            Integer userId, Integer examId, Integer pathId, String requestKey);

    Optional<Integer> checkpointOriginPathId(Integer userId, Integer attemptId);

    void submit(Integer userId, Integer attemptId, String requestKey);

    AttemptResult result(Integer userId, Integer attemptId);

    HistoryPage history(Integer userId, int page, int size);

    HistoryPage history(Integer userId, int page, int size, HistoryFilter filter);

    AdminHistoryPage adminHistory(String adminEmail, int page, int size, AdminHistoryFilter filter);

    AttemptResult adminResult(String adminEmail, Integer attemptId);

    void retryFailedGrading(Integer userId, Integer attemptId, String requestKey);

    boolean canRegradeEmpty(Integer userId, Integer attemptId);

    void regradeEmpty(Integer userId, Integer attemptId);

    void saveSpeakingAudio(
            Integer userId,
            Integer attemptId,
            Integer questionId,
            Integer itemId,
            MultipartFile audioFile);

    MediaPlayResult playMedia(Integer userId, Integer attemptId, Integer mediaId);

    MediaResource mediaContent(Integer userId, Integer attemptId, Integer mediaId, String token);

    MediaResource speakingAudio(Integer userId, Integer attemptId, Integer answerId);

    void saveAnswer(
            Integer userId,
            Integer attemptId,
            Integer questionId,
            Integer itemId,
            Integer selectedOptionId,
            Integer userPosition,
            String textAnswer);

    void discard(Integer userId, Integer attemptId);

    void advanceSection(Integer userId, Integer attemptId, int sectionEndMinute);

    AttemptReview review(Integer userId, Integer attemptId);

    AttemptReview adminReview(String adminEmail, Integer attemptId);

    void adminDiscard(String adminEmail, Integer attemptId);

    MediaResource reviewMedia(Integer userId, Integer attemptId, Integer mediaId);

    MediaResource reviewRecording(
            Integer userId, Integer attemptId, Integer questionId, Integer itemId);

    AttemptPage page(Integer userId, Integer attemptId);

    void preserveLegacyAttempts(Integer examId);

    void validateExamReady(MockExamEntity exam, List<MockExamQuestionEntity> links);

    void validateExamForPublication(MockExamEntity exam, List<MockExamQuestionEntity> links);

    List<java.time.LocalDateTime> findSubmittedDatesByUserId(Integer userId);

    record SnapshotSchemeInfo(Integer scoringSchemeId, String skill) {}

    Optional<SnapshotSchemeInfo> findSnapshotSchemeInfo(Integer attemptId);

    record SectionPage(
            int index,
            String skill,
            int durationMinutes,
            int questionCount,
            String state,
            boolean open) {}

    record AttemptPage(
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

    record QuestionPage(
            Integer questionId,
            String content,
            String instruction,
            String readingPassage,
            String skill,
            String type,
            int position,
            List<ItemPage> items,
            List<MediaPage> media) {}

    record ItemPage(
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

    record OptionPage(Integer id, String code, String content, @JsonIgnore boolean correct) {}

    record MediaPage(Integer id, String type, String filename, String contentType) {}

    record MediaPlayResult(
            Integer mediaId,
            int playCount,
            int maxPlayCount,
            String contentToken,
            boolean browserSpeech,
            String speechText) {}

    record MediaResource(Resource resource, String contentType) {}

    record AttemptReview(String title, List<QuestionPage> questions) {}
}

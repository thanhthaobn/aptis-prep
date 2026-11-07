package com.aptisprep.service.exam;

import com.aptisprep.entity.*;
import com.aptisprep.exception.*;
import com.aptisprep.repository.*;
import com.aptisprep.service.contract.GradingTextLimits;
import com.aptisprep.service.storage.AudioUploadService;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Answer persistence only. Caller holds the owner-bound attempt lock and transaction. */
@Component
public class ExamAnswerWriter {
    private final QuestionRepository questionRepository;
    private final QuestionItemRepository questionItemRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final UserAnswerDetailRepository userAnswerDetailRepository;
    private final ExamSnapshotService examSnapshots;
    private final AudioUploadService audioUploads;
    private final JsonMapper json;

    public ExamAnswerWriter(
            QuestionRepository questionRepository,
            QuestionItemRepository questionItemRepository,
            UserAnswerRepository userAnswerRepository,
            UserAnswerDetailRepository userAnswerDetailRepository,
            ExamSnapshotService examSnapshots,
            AudioUploadService audioUploads,
            JsonMapper json) {
        this.questionRepository = questionRepository;
        this.questionItemRepository = questionItemRepository;
        this.userAnswerRepository = userAnswerRepository;
        this.userAnswerDetailRepository = userAnswerDetailRepository;
        this.examSnapshots = examSnapshots;
        this.audioUploads = audioUploads;
        this.json = json;
    }

    void saveSpeakingAudio(
            ExamAttemptEntity attempt,
            Integer questionId,
            Integer itemId,
            MultipartFile audioFile) {
        Integer attemptId = attempt.getId();
        QuestionEntity question =
                questionRepository
                        .findById(questionId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        if (!"SPEAKING_AUDIO".equals(question.getQuestionType()))
            throw new BusinessRuleException("Câu hỏi không yêu cầu file ghi âm");
        UserAnswerEntity answer =
                userAnswerRepository
                        .findByAttemptIdAndQuestionId(attemptId, questionId)
                        .orElseThrow(
                                () ->
                                        new BusinessRuleException(
                                                "Câu hỏi không thuộc lượt thi này"));
        JsonNode lockedQuestion = examSnapshots.lockedQuestions(attemptId).get(questionId);
        ExamSnapshotService.requireOpenSection(attempt, lockedQuestion);
        JsonNode lockedItem = chooseLockedItem(lockedQuestion, itemId);
        QuestionItemEntity item =
                questionItemRepository
                        .findById(lockedItem.path("id").asInt())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Mục câu hỏi không còn tồn tại"));
        String path = audioUploads.store(audioFile, attemptId);
        UserAnswerDetailEntity detail =
                userAnswerDetailRepository
                        .findByUserAnswerIdAndQuestionItemId(answer.getId(), item.getId())
                        .orElseGet(
                                () -> {
                                    UserAnswerDetailEntity value = new UserAnswerDetailEntity();
                                    value.setUserAnswerId(answer.getId());
                                    value.setQuestionItemId(item.getId());
                                    return value;
                                });
        detail.setAudioPath(path);
        detail.setGradingStatus("PENDING");
        answer.setStatus("ANSWERED");
        if (answer.getStartedAt() == null) answer.setStartedAt(LocalDateTime.now());
        answer.setAnswerVersion(answer.getAnswerVersion() + 1);
        userAnswerDetailRepository.saveAndFlush(detail);
        syncSubjectiveContent(answer, "SPEAKING_AUDIO");
        userAnswerRepository.save(answer);
    }

    void saveAnswer(
            ExamAttemptEntity attempt,
            Integer questionId,
            Integer itemId,
            Integer selectedOptionId,
            Integer userPosition,
            String textAnswer) {
        Integer attemptId = attempt.getId();
        UserAnswerEntity answer =
                userAnswerRepository
                        .findByAttemptIdAndQuestionId(attemptId, questionId)
                        .orElseThrow(
                                () ->
                                        new BusinessRuleException(
                                                "Câu hỏi không thuộc lượt thi này"));
        JsonNode lockedQuestion = examSnapshots.lockedQuestions(attemptId).get(questionId);
        if (lockedQuestion == null)
            throw new BusinessRuleException("Câu hỏi không thuộc snapshot của lượt thi");
        ExamSnapshotService.requireOpenSection(attempt, lockedQuestion);
        JsonNode lockedItem = chooseLockedItem(lockedQuestion, itemId);
        QuestionItemEntity item =
                questionItemRepository
                        .findById(lockedItem.path("id").asInt())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Mục câu hỏi không còn tồn tại"));
        String questionType = lockedQuestion.path("type").asText();
        UserAnswerDetailEntity detail =
                userAnswerDetailRepository
                        .findByUserAnswerIdAndQuestionItemId(answer.getId(), item.getId())
                        .orElseGet(
                                () -> {
                                    UserAnswerDetailEntity value = new UserAnswerDetailEntity();
                                    value.setUserAnswerId(answer.getId());
                                    value.setQuestionItemId(item.getId());
                                    return value;
                                });
        if ("ORDERING".equals(questionType)) {
            if (userPosition != null
                    && (userPosition < 1 || userPosition > lockedQuestion.path("items").size()))
                throw new BusinessRuleException("Vị trí sắp xếp không hợp lệ");
            detail.setUserPosition(userPosition);
        } else if (usesOptions(questionType)) {
            if (selectedOptionId != null && !hasLockedOption(lockedItem, selectedOptionId)) {
                throw new BusinessRuleException("Phương án không thuộc câu hỏi");
            }
            detail.setSelectedOptionId(selectedOptionId);
        } else if ("WRITING_TEXT".equals(questionType)) {
            String value = textAnswer == null ? "" : textAnswer.trim();
            if (value.length() > GradingTextLimits.MAX_ITEM_CHARS) {
                throw new BusinessRuleException(
                        "Bài viết ở mục "
                                + item.getPosition()
                                + " vượt quá "
                                + GradingTextLimits.MAX_ITEM_CHARS
                                + " ký tự");
            }
            // The detail row is the source of truth: a question may contain
            // several writing items, so an answer-level field must not replace
            // the text of another item.
            // Item rows retain the editable structure; the answer-level representation
            // is rebuilt in the same transaction and is the grading source.
            detail.setAnswerText(value);
        } else if ("SPEAKING_AUDIO".equals(questionType)) {
            throw new BusinessRuleException("Speaking phải tải file bằng multipart upload");
        }
        if (answer.getStartedAt() == null) answer.setStartedAt(LocalDateTime.now());
        answer.setAnswerVersion(answer.getAnswerVersion() + 1);
        answer.setStatus("ANSWERED");
        userAnswerDetailRepository.saveAndFlush(detail);
        syncSubjectiveContent(answer, questionType);
        userAnswerRepository.save(answer);
    }

    private void syncSubjectiveContent(UserAnswerEntity answer, String type) {
        List<UserAnswerDetailEntity> values =
                userAnswerDetailRepository.findByUserAnswerIdOrderByQuestionItemIdAsc(
                        answer.getId());
        if ("WRITING_TEXT".equals(type)) {
            answer.setTextAnswer(
                    values.stream()
                            .map(UserAnswerDetailEntity::getAnswerText)
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining("\n--- ITEM ---\n")));
        } else if ("SPEAKING_AUDIO".equals(type)) {
            List<String> paths =
                    values.stream()
                            .map(UserAnswerDetailEntity::getAudioPath)
                            .filter(Objects::nonNull)
                            .filter(path -> !path.isBlank())
                            .toList();
            answer.setAudioUrl(paths.isEmpty() ? null : paths.getFirst());
            answer.setAudioManifest(
                    json.writeValueAsString(
                            paths.stream().map(path -> Map.of("path", path)).toList()));
        }
    }

    private JsonNode chooseLockedItem(JsonNode question, Integer itemId) {
        if (question == null || question.isMissingNode())
            throw new BusinessRuleException("Câu hỏi không thuộc snapshot của lượt thi");
        for (JsonNode item : question.path("items"))
            if (itemId == null || item.path("id").asInt() == itemId) return item;
        throw new ResourceNotFoundException("Mục câu hỏi không thuộc câu hỏi");
    }

    private boolean hasLockedOption(JsonNode item, Integer optionId) {
        for (JsonNode option : item.path("options"))
            if (option.path("id").asInt() == optionId) return true;
        return false;
    }

    private boolean usesOptions(String type) {
        return "SINGLE_CHOICE".equals(type) || "MATCHING".equals(type) || "ORDERING".equals(type);
    }
}

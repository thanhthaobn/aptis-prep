package com.aptisprep.mvc.service.exam.impl;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.exception.*;
import com.aptisprep.mvc.repository.*;
import com.aptisprep.mvc.service.exam.ExamSnapshotService;
import com.aptisprep.mvc.service.exam.ExamAnswerService;
import com.aptisprep.mvc.support.contract.GradingTextLimits;
import com.aptisprep.mvc.service.storage.AudioUploadService;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Chỉ lưu đáp án. Caller phải giữ khóa attempt theo chủ sở hữu và transaction. */
import org.springframework.stereotype.Service;

@Service
public class ExamAnswerServiceImpl implements ExamAnswerService {
    private QuestionRepository questionRepository;
    private QuestionItemRepository questionItemRepository;
    private UserAnswerRepository userAnswerRepository;
    private UserAnswerDetailRepository userAnswerDetailRepository;
    private ExamSnapshotService examSnapshots;
    private AudioUploadService audioUploads;

    public ExamAnswerServiceImpl(
            QuestionRepository questionRepository,
            QuestionItemRepository questionItemRepository,
            UserAnswerRepository userAnswerRepository,
            UserAnswerDetailRepository userAnswerDetailRepository,
            ExamSnapshotService examSnapshots,
            AudioUploadService audioUploads) {
        this.questionRepository = questionRepository;
        this.questionItemRepository = questionItemRepository;
        this.userAnswerRepository = userAnswerRepository;
        this.userAnswerDetailRepository = userAnswerDetailRepository;
        this.examSnapshots = examSnapshots;
        this.audioUploads = audioUploads;
    }

    @Override
    @Transactional
    public void saveSpeakingAudio(
            ExamAttempt attempt,
            Integer questionId,
            Integer itemId,
            MultipartFile audioFile) {
        Integer attemptId = attempt.getId();
        Question question =
                questionRepository
                        .findById(questionId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        if (!"SPEAKING_AUDIO".equals(question.getQuestionType()))
            throw new BusinessRuleException("Câu hỏi không yêu cầu file ghi âm");
        UserAnswer answer =
                userAnswerRepository
                        .findByAttemptIdAndQuestionId(attemptId, questionId)
                        .orElseThrow(
                                () ->
                                        new BusinessRuleException(
                                                "Câu hỏi không thuộc lượt thi này"));
        ExamQuestionSnapshot lockedQuestion =
                examSnapshots.lockedQuestionEntity(attemptId, questionId);
        examSnapshots.requireOpenSection(attempt, lockedQuestion);
        ExamItemSnapshot lockedItem = chooseLockedItem(lockedQuestion, itemId);
        QuestionItem item =
                questionItemRepository
                        .findById(lockedItem.getSourceItemId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Mục câu hỏi không còn tồn tại"));
        String path = audioUploads.store(audioFile, attemptId);
        UserAnswerDetail detail =
                userAnswerDetailRepository
                        .findByUserAnswerIdAndQuestionItemId(answer.getId(), item.getId())
                        .orElseGet(
                                () -> {
                                    UserAnswerDetail value = new UserAnswerDetail();
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

    @Override
    @Transactional
    public void saveAnswer(
            ExamAttempt attempt,
            Integer questionId,
            Integer itemId,
            Integer selectedOptionId,
            Integer userPosition,
            String textAnswer) {
        Integer attemptId = attempt.getId();
        UserAnswer answer =
                userAnswerRepository
                        .findByAttemptIdAndQuestionId(attemptId, questionId)
                        .orElseThrow(
                                () ->
                                        new BusinessRuleException(
                                                "Câu hỏi không thuộc lượt thi này"));
        ExamQuestionSnapshot lockedQuestion =
                examSnapshots.lockedQuestionEntity(attemptId, questionId);
        if (lockedQuestion == null)
            throw new BusinessRuleException("Câu hỏi không thuộc snapshot của lượt thi");
        examSnapshots.requireOpenSection(attempt, lockedQuestion);
        ExamItemSnapshot lockedItem = chooseLockedItem(lockedQuestion, itemId);
        QuestionItem item =
                questionItemRepository
                        .findById(lockedItem.getSourceItemId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Mục câu hỏi không còn tồn tại"));
        String questionType = lockedQuestion.getQuestionType();
        UserAnswerDetail detail =
                userAnswerDetailRepository
                        .findByUserAnswerIdAndQuestionItemId(answer.getId(), item.getId())
                        .orElseGet(
                                () -> {
                                    UserAnswerDetail value = new UserAnswerDetail();
                                    value.setUserAnswerId(answer.getId());
                                    value.setQuestionItemId(item.getId());
                                    return value;
                                });
        if ("ORDERING".equals(questionType)) {
            if (userPosition != null
                    && (userPosition < 1 || userPosition > lockedQuestion.getItems().size()))
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
            Integer maxWords = item.getMaxWordCount();
            if (maxWords != null && wordCount(value) > maxWords) {
                throw new BusinessRuleException(
                        "Bài viết ở mục " + item.getPosition() + " vượt quá " + maxWords + " từ");
            }
            // Row detail là nguồn dữ liệu chuẩn: một câu hỏi có thể chứa nhiều writing item,
            // nên field ở cấp answer không được ghi đè nội dung của item khác.
            // Các row item giữ cấu trúc có thể chỉnh sửa; biểu diễn ở cấp answer được
            // dựng lại trong cùng transaction và là nguồn dùng để chấm điểm.
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

    private void syncSubjectiveContent(UserAnswer answer, String type) {
        List<UserAnswerDetail> values =
                userAnswerDetailRepository.findByUserAnswerIdOrderByQuestionItemIdAsc(
                        answer.getId());
        if ("WRITING_TEXT".equals(type)) {
            answer.setTextAnswer(
                    values.stream()
                            .map(streamValue -> streamValue.getAnswerText())
                            .filter(streamValue -> Objects.nonNull(streamValue))
                            .collect(Collectors.joining("\n--- ITEM ---\n")));
        } else if ("SPEAKING_AUDIO".equals(type)) {
            List<String> paths =
                    values.stream()
                            .map(streamValue -> streamValue.getAudioPath())
                            .filter(streamValue -> Objects.nonNull(streamValue))
                            .filter(path -> !path.isBlank())
                            .toList();
            answer.setAudioUrl(paths.isEmpty() ? null : paths.getFirst());
            answer.setAudioManifest(String.join("\n", paths));
        }
    }

    private ExamItemSnapshot chooseLockedItem(
            ExamQuestionSnapshot question, Integer itemId) {
        if (question == null)
            throw new BusinessRuleException("Câu hỏi không thuộc snapshot của lượt thi");
        for (ExamItemSnapshot item : question.getItems())
            if (itemId == null || item.getSourceItemId().equals(itemId)) return item;
        throw new ResourceNotFoundException("Mục câu hỏi không thuộc câu hỏi");
    }

    private boolean hasLockedOption(ExamItemSnapshot item, Integer optionId) {
        for (ExamOptionSnapshot option : item.getOptions())
            if (option.getSourceOptionId().equals(optionId)) return true;
        return false;
    }

    private int wordCount(String value) {
        return value == null || value.isBlank() ? 0 : value.trim().split("\\s+").length;
    }

    private boolean usesOptions(String type) {
        return "SINGLE_CHOICE".equals(type) || "MATCHING".equals(type) || "ORDERING".equals(type);
    }
}








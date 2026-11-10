package com.aptisprep.service.exam;

import static com.aptisprep.service.contract.GradingContextFormatter.DEFAULT_RUBRIC_VERSION;

import com.aptisprep.entity.*;
import com.aptisprep.mvc.common.exception.BusinessRuleException;
import com.aptisprep.repository.UserAnswerDetailRepository;
import com.aptisprep.repository.UserAnswerRepository;
import com.aptisprep.service.contract.*;
import com.aptisprep.service.contract.IntegrationModels.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Builds grading DTOs from locked content and persisted answers. Never writes attempt state. */
@Component
final class ExamGradingPayloadFactory {
    private final ExamSnapshotService examSnapshots;
    private final UserAnswerRepository userAnswerRepository;
    private final UserAnswerDetailRepository userAnswerDetailRepository;

    ExamGradingPayloadFactory(
            ExamSnapshotService examSnapshots,
            UserAnswerRepository userAnswerRepository,
            UserAnswerDetailRepository userAnswerDetailRepository) {
        this.examSnapshots = examSnapshots;
        this.userAnswerRepository = userAnswerRepository;
        this.userAnswerDetailRepository = userAnswerDetailRepository;
    }

    LockedAttempt build(ExamAttemptEntity attempt) {
        ExamAttemptSnapshotEntity snapshot = examSnapshots.require(attempt.getId());
        Map<Integer, ExamQuestionSnapshotEntity> lockedByQuestion =
                examSnapshots.lockedQuestionEntities(attempt.getId()).stream()
                        .collect(
                                Collectors.toMap(
                                        ExamQuestionSnapshotEntity::getSourceQuestionId,
                                        value -> value,
                                        (left, right) -> left,
                                        LinkedHashMap::new));
        List<LockedAnswer> lockedAnswers =
                userAnswerRepository.findByAttemptIdOrderByQuestionIdAsc(attempt.getId()).stream()
                        .map(
                                answer -> {
                                    ExamQuestionSnapshotEntity question =
                                            lockedByQuestion.get(answer.getQuestionId());
                                    if (question == null)
                                        throw new BusinessRuleException(
                                                "Snapshot không chứa câu hỏi của lượt thi");
                                    String instruction =
                                            Objects.requireNonNullElse(
                                                    question.getInstruction(), "");
                                    String content =
                                            Objects.requireNonNullElse(question.getContent(), "");
                                    String task =
                                            instruction.isBlank()
                                                    ? content
                                                    : instruction + "\n" + content;
                                    List<UserAnswerDetailEntity> answerDetails =
                                            userAnswerDetailRepository
                                                    .findByUserAnswerIdOrderByQuestionItemIdAsc(
                                                            answer.getId());
                                    String legacyLockedContent =
                                            firstNonBlank(
                                                    answer.getTextAnswer(),
                                                    answer.getAudioUrl(),
                                                    answer.getTranscript());
                                    if ("SPEAKING_AUDIO".equals(question.getQuestionType())
                                            && answer.getAudioManifest() != null) {
                                        List<String> paths =
                                                Arrays.stream(
                                                                answer.getAudioManifest()
                                                                        .split("\\R"))
                                                        .filter(value -> !value.isBlank())
                                                        .toList();
                                        legacyLockedContent =
                                                String.join("\n--- ITEM ---\n", paths);
                                    }
                                    // Read compatibility for historical answers; existing detail
                                    // data is preserved.
                                    if (legacyLockedContent == null
                                            || legacyLockedContent.isBlank())
                                        legacyLockedContent =
                                                answerDetails.stream()
                                                        .map(
                                                                detail ->
                                                                        firstNonBlank(
                                                                                detail
                                                                                        .getAnswerText(),
                                                                                detail
                                                                                        .getAudioPath(),
                                                                                detail
                                                                                        .getTranscript()))
                                                        .filter(Objects::nonNull)
                                                        .collect(
                                                                Collectors.joining(
                                                                        "\n--- ITEM ---\n"));
                                    List<LockedItem> lockedItems = new ArrayList<>();
                                    for (ExamItemSnapshotEntity item : question.getItems()) {
                                        UserAnswerDetailEntity detail =
                                                userAnswerDetailRepository
                                                        .findByUserAnswerIdAndQuestionItemId(
                                                                answer.getId(),
                                                                item.getSourceItemId())
                                                        .orElse(null);
                                        String itemContent =
                                                detail == null
                                                        ? null
                                                        : firstNonBlank(
                                                                detail.getAnswerText(),
                                                                detail.getAudioPath(),
                                                                detail.getTranscript());
                                        Integer correctOption = null;
                                        for (ExamOptionSnapshotEntity option : item.getOptions()) {
                                            if (Boolean.TRUE.equals(option.getCorrect())) {
                                                correctOption = option.getSourceOptionId();
                                                break;
                                            }
                                        }
                                        lockedItems.add(
                                                new LockedItem(
                                                        item.getSourceItemId(),
                                                        item.getPosition(),
                                                        itemPrompt(item),
                                                        itemContent,
                                                        correctOption,
                                                        item.getCorrectPosition()));
                                    }
                                    String lockedContent =
                                            lockedItems.stream()
                                                    .map(LockedItem::lockedContent)
                                                    .filter(Objects::nonNull)
                                                    .filter(value -> !value.isBlank())
                                                    .collect(
                                                            Collectors.joining("\n--- ITEM ---\n"));
                                    if (lockedContent.isBlank())
                                        lockedContent = legacyLockedContent;
                                    return new LockedAnswer(
                                            answer.getId(),
                                            answer.getQuestionId(),
                                            question.getSkill(),
                                            question.getQuestionType(),
                                            task,
                                            lockedContent,
                                            answer.getTranscript(),
                                            lockedItems);
                                })
                        .toList();
        return new LockedAttempt(
                attempt.getId(),
                attempt.getUserId(),
                snapshot.getExamId(),
                snapshot.getExamType(),
                attempt.getAnswerVersion() == null ? 1 : attempt.getAnswerVersion(),
                instant(attempt.getSubmittedAt()),
                attempt.getScoringSchemeId() == null
                        ? snapshot.getScoringSchemeId()
                        : attempt.getScoringSchemeId(),
                attempt.getPolicyVersion(),
                attempt.getRubricVersion() == null
                        ? DEFAULT_RUBRIC_VERSION
                        : attempt.getRubricVersion(),
                snapshot.getPolicyVersion(),
                attempt.getGradingRequestKey(),
                lockedAnswers);
    }

    void validate(LockedAttempt locked) {
        for (LockedAnswer answer : locked.answers()) {
            if (!Set.of("WRITING_TEXT", "SPEAKING_AUDIO").contains(answer.questionType())) continue;
            String task = GradingContextFormatter.task(answer);
            String submission = GradingContextFormatter.submission(answer);
            for (LockedItem item : answer.items()) {
                if (GradingTextLimits.length(item.lockedContent())
                        > GradingTextLimits.MAX_ITEM_CHARS) {
                    throw new BusinessRuleException(
                            "Bài làm ở mục "
                                    + item.position()
                                    + " vượt quá "
                                    + GradingTextLimits.MAX_ITEM_CHARS
                                    + " ký tự");
                }
            }
            if (GradingTextLimits.length(task) + GradingTextLimits.length(submission)
                    > GradingTextLimits.MAX_QUESTION_CHARS) {
                throw new BusinessRuleException(
                        "Nội dung câu "
                                + answer.questionId()
                                + " vượt quá "
                                + GradingTextLimits.MAX_QUESTION_CHARS
                                + " ký tự cho phép");
            }
            if (GradingTextLimits.length(task) > GradingTextLimits.MAX_AI_TASK_CHARS
                    || GradingTextLimits.length(submission)
                            > GradingTextLimits.MAX_AI_SUBMISSION_CHARS
                    || GradingTextLimits.length(task) + GradingTextLimits.length(submission)
                            > GradingTextLimits.MAX_AI_PAYLOAD_CHARS) {
                throw new BusinessRuleException(
                        "Nội dung câu " + answer.questionId() + " vượt giới hạn ngữ cảnh chấm AI");
            }
        }
    }

    private String itemPrompt(ExamItemSnapshotEntity item) {
        StringBuilder prompt = new StringBuilder(Objects.requireNonNullElse(item.getContent(), ""));
        if (item.getMinWordCount() != null || item.getMaxWordCount() != null) {
            prompt.append("\nGiới hạn từ: ");
            if (item.getMinWordCount() != null)
                prompt.append("tối thiểu ").append(item.getMinWordCount()).append(" ");
            if (item.getMaxWordCount() != null)
                prompt.append("tối đa ").append(item.getMaxWordCount()).append(" ");
            prompt.append("từ.");
        }
        if (item.getResponseSeconds() != null)
            prompt.append("\nThời gian trả lời: ")
                    .append(item.getResponseSeconds())
                    .append(" giây.");
        return prompt.toString();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return null;
    }

    private Instant instant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }
}

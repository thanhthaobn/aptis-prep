package com.aptisprep.service.exam;

import static com.aptisprep.service.contract.GradingContextFormatter.DEFAULT_RUBRIC_VERSION;

import com.aptisprep.entity.*;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.repository.UserAnswerDetailRepository;
import com.aptisprep.repository.UserAnswerRepository;
import com.aptisprep.service.contract.*;
import com.aptisprep.service.contract.IntegrationModels.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Builds grading DTOs from locked content and persisted answers. Never writes attempt state. */
@Component
final class ExamGradingPayloadFactory {
    private final ExamSnapshotService examSnapshots;
    private final UserAnswerRepository answers;
    private final UserAnswerDetailRepository details;
    private final JsonMapper json;

    ExamGradingPayloadFactory(
            ExamSnapshotService examSnapshots,
            UserAnswerRepository answers,
            UserAnswerDetailRepository details,
            JsonMapper json) {
        this.examSnapshots = examSnapshots;
        this.answers = answers;
        this.details = details;
        this.json = json;
    }

    LockedAttempt build(ExamAttemptEntity attempt, MockExamEntity exam) {
        ExamAttemptSnapshotEntity snapshot = examSnapshots.require(attempt.getId());
        Map<Integer, JsonNode> lockedByQuestion = examSnapshots.lockedQuestions(attempt.getId());
        List<LockedAnswer> lockedAnswers =
                answers.findByAttemptIdOrderByQuestionIdAsc(attempt.getId()).stream()
                        .map(
                                answer -> {
                                    JsonNode question =
                                            lockedByQuestion.get(answer.getQuestionId());
                                    if (question == null)
                                        throw new BusinessRuleException(
                                                "Snapshot không chứa câu hỏi của lượt thi");
                                    String instruction = question.path("instruction").asText("");
                                    String content = question.path("content").asText("");
                                    String task =
                                            instruction.isBlank()
                                                    ? content
                                                    : instruction + "\n" + content;
                                    List<UserAnswerDetailEntity> answerDetails =
                                            details.findByUserAnswerIdOrderByQuestionItemIdAsc(
                                                    answer.getId());
                                    String legacyLockedContent =
                                            firstNonBlank(
                                                    answer.getTextAnswer(),
                                                    answer.getAudioUrl(),
                                                    answer.getTranscript());
                                    if ("SPEAKING_AUDIO".equals(question.path("type").asText())
                                            && answer.getAudioManifest() != null) {
                                        List<String> paths = new ArrayList<>();
                                        for (JsonNode entry :
                                                json.readTree(answer.getAudioManifest()))
                                            paths.add(entry.path("path").asText());
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
                                    for (JsonNode item : question.path("items")) {
                                        UserAnswerDetailEntity detail =
                                                details.findByUserAnswerIdAndQuestionItemId(
                                                                answer.getId(),
                                                                item.path("id").asInt())
                                                        .orElse(null);
                                        String itemContent =
                                                detail == null
                                                        ? null
                                                        : firstNonBlank(
                                                                detail.getAnswerText(),
                                                                detail.getAudioPath(),
                                                                detail.getTranscript());
                                        Integer correctOption = null;
                                        for (JsonNode option : item.path("options")) {
                                            if (option.path("correct").asBoolean(false)) {
                                                correctOption = option.path("id").asInt();
                                                break;
                                            }
                                        }
                                        lockedItems.add(
                                                new LockedItem(
                                                        item.path("id").asInt(),
                                                        item.path("position").asInt(),
                                                        itemPrompt(item),
                                                        itemContent,
                                                        correctOption,
                                                        item.path("correctPosition").isNull()
                                                                ? null
                                                                : item.path("correctPosition")
                                                                        .asInt()));
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
                                            question.path("skill").asText(),
                                            question.path("type").asText(),
                                            task,
                                            lockedContent,
                                            answer.getTranscript(),
                                            lockedItems);
                                })
                        .toList();
        return new LockedAttempt(
                attempt.getId(),
                attempt.getUserId(),
                exam.getId(),
                exam.getExamType(),
                attempt.getAnswerVersion() == null ? 1 : attempt.getAnswerVersion(),
                instant(attempt.getSubmittedAt()),
                attempt.getScoringSchemeId() == null
                        ? exam.getScoringSchemeId()
                        : attempt.getScoringSchemeId(),
                attempt.getPolicyVersion(),
                attempt.getRubricVersion() == null
                        ? DEFAULT_RUBRIC_VERSION
                        : attempt.getRubricVersion(),
                snapshot.getGradingContext(),
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

    private String itemPrompt(JsonNode item) {
        StringBuilder prompt = new StringBuilder(item.path("content").asText(""));
        if (item.path("minWordCount").isNumber() || item.path("maxWordCount").isNumber()) {
            prompt.append("\nGiới hạn từ: ");
            if (item.path("minWordCount").isNumber())
                prompt.append("tối thiểu ").append(item.path("minWordCount").asInt()).append(" ");
            if (item.path("maxWordCount").isNumber())
                prompt.append("tối đa ").append(item.path("maxWordCount").asInt()).append(" ");
            prompt.append("từ.");
        }
        if (item.path("responseSeconds").isNumber())
            prompt.append("\nThời gian trả lời: ")
                    .append(item.path("responseSeconds").asInt())
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

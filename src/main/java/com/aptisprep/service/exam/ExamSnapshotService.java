package com.aptisprep.service.exam;

import com.aptisprep.entity.*;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Owns immutable exam content snapshots; writes join the caller's attempt transaction. */
@Service
final class ExamSnapshotService {
    private final QuestionRepository questions;
    private final QuestionItemRepository items;
    private final QuestionOptionRepository options;
    private final QuestionMediaRepository questionMedia;
    private final ExamAttemptSnapshotRepository snapshots;
    private final JsonMapper json;

    ExamSnapshotService(
            QuestionRepository questions,
            QuestionItemRepository items,
            QuestionOptionRepository options,
            QuestionMediaRepository questionMedia,
            ExamAttemptSnapshotRepository snapshots,
            JsonMapper json) {
        this.questions = questions;
        this.items = items;
        this.options = options;
        this.questionMedia = questionMedia;
        this.snapshots = snapshots;
        this.json = json;
    }

    ExamAttemptSnapshotEntity require(Integer attemptId) {
        return snapshots
                .findByAttemptId(attemptId)
                .orElseThrow(
                        () -> new BusinessRuleException("Không tìm thấy snapshot của lượt thi"));
    }

    void updateAnswerVersion(Integer attemptId, int version) {
        snapshots
                .findByAttemptId(attemptId)
                .ifPresent(
                        snapshot -> {
                            snapshot.setAnswerVersion(version);
                            snapshots.save(snapshot);
                        });
    }

    void createSnapshot(
            ExamAttemptEntity attempt, MockExamEntity exam, List<MockExamQuestionEntity> links) {
        ExamAttemptSnapshotEntity snapshot = new ExamAttemptSnapshotEntity();
        snapshot.setAttemptId(attempt.getId());
        snapshot.setExamId(exam.getId());
        snapshot.setCertificateId(exam.getCertificateId());
        snapshot.setScoringSchemeId(exam.getScoringSchemeId());
        snapshot.setExamType(exam.getExamType());
        snapshot.setSkill(exam.getSkill());
        snapshot.setTargetLevel(exam.getTargetLevel());
        snapshot.setTitle(exam.getTitle());
        snapshot.setDurationMinutes(exam.getDurationMinutes());
        snapshot.setAnswerVersion(1);
        snapshot.setPolicyVersion(attempt.getPolicyVersion());
        Map<String, Object> grading = new LinkedHashMap<>();
        grading.put("certificateId", exam.getCertificateId());
        grading.put("scoringSchemeId", exam.getScoringSchemeId());
        grading.put("policyVersion", snapshot.getPolicyVersion());
        grading.put("answerVersion", 1);
        List<Map<String, Object>> lockedQuestions = new ArrayList<>();
        for (MockExamQuestionEntity link : links) {
            QuestionEntity question = questions.findById(link.getQuestionId()).orElseThrow();
            Map<String, Object> q = new LinkedHashMap<>();
            q.put("id", question.getId());
            q.put("position", link.getPosition());
            q.put("content", question.getContent());
            q.put("instruction", question.getInstruction());
            q.put("readingPassage", question.getReadingPassage());
            q.put("type", question.getQuestionType());
            q.put("skill", question.getSkill());
            List<Map<String, Object>> lockedItems = new ArrayList<>();
            for (QuestionItemEntity item :
                    items.findByQuestionIdOrderByPosition(question.getId())) {
                Map<String, Object> i = new LinkedHashMap<>();
                i.put("id", item.getId());
                i.put("position", item.getPosition());
                i.put("content", item.getContent());
                i.put("correctPosition", item.getCorrectPosition());
                i.put("minWordCount", item.getMinWordCount());
                i.put("maxWordCount", item.getMaxWordCount());
                i.put("responseSeconds", item.getResponseSeconds());
                List<Map<String, Object>> lockedOptions = new ArrayList<>();
                options.findByQuestionItemIdOrderByPosition(item.getId())
                        .forEach(
                                option -> {
                                    Map<String, Object> o = new LinkedHashMap<>();
                                    o.put("id", option.getId());
                                    o.put("code", option.getOptionCode());
                                    o.put("position", option.getPosition());
                                    o.put("content", option.getContent());
                                    o.put("correct", option.getCorrect());
                                    lockedOptions.add(o);
                                });
                i.put("options", lockedOptions);
                lockedItems.add(i);
            }
            List<Map<String, Object>> lockedMedia = new ArrayList<>();
            for (QuestionMediaEntity media :
                    questionMedia.findByQuestionIdOrderByPosition(question.getId())) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", media.getId());
                m.put("questionId", question.getId());
                m.put("type", media.getMediaType());
                m.put("mediaUrl", media.getMediaUrl());
                m.put("filename", media.getOriginalFilename());
                m.put("contentType", media.getContentType());
                m.put("transcript", media.getTranscript());
                m.put(
                        "maxPlayCount",
                        media.getMaxPlayCount() == null ? 2 : media.getMaxPlayCount());
                lockedMedia.add(m);
            }
            q.put("items", lockedItems);
            q.put("media", lockedMedia);
            lockedQuestions.add(q);
        }
        try {
            snapshot.setGradingContext(json.writeValueAsString(grading));
            snapshot.setLockedQuestionSnapshot(json.writeValueAsString(lockedQuestions));
        } catch (RuntimeException exception) {
            throw new BusinessRuleException("Không thể khóa nội dung đề thi cho lượt làm bài");
        }
        snapshots.save(snapshot);
    }

    Map<Integer, JsonNode> lockedQuestions(Integer attemptId) {
        ExamAttemptSnapshotEntity snapshot =
                snapshots
                        .findByAttemptId(attemptId)
                        .orElseThrow(
                                () ->
                                        new BusinessRuleException(
                                                "Không tìm thấy snapshot của lượt thi"));
        try {
            Map<Integer, JsonNode> result = new LinkedHashMap<>();
            for (JsonNode question : json.readTree(snapshot.getLockedQuestionSnapshot()))
                result.put(question.path("id").asInt(), question);
            return result;
        } catch (RuntimeException exception) {
            throw new BusinessRuleException("Snapshot lượt thi không hợp lệ");
        }
    }

    List<JsonNode> lockedQuestionList(Integer attemptId) {
        return lockedQuestions(attemptId).values().stream()
                .sorted(Comparator.comparingInt(value -> value.path("position").asInt()))
                .toList();
    }

    JsonNode lockedMedia(Integer attemptId, Integer mediaId) {
        for (JsonNode question : lockedQuestionList(attemptId)) {
            for (JsonNode media : question.path("media"))
                if (media.path("id").asInt() == mediaId) return media;
        }
        return null;
    }
}

package com.aptisprep.service.exam;

import com.aptisprep.entity.*;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Owns immutable exam content snapshots; writes join the caller's attempt transaction. */
@Service
final class ExamSnapshotService {
    private final QuestionRepository questionRepository;
    private final QuestionItemRepository questionItemRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final QuestionMediaRepository questionMediaRepository;
    private final ExamAttemptSnapshotRepository examAttemptSnapshotRepository;
    private final MockExamSectionRepository sections;
    private final JsonMapper json;

    ExamSnapshotService(
            QuestionRepository questionRepository,
            QuestionItemRepository questionItemRepository,
            QuestionOptionRepository questionOptionRepository,
            QuestionMediaRepository questionMediaRepository,
            ExamAttemptSnapshotRepository examAttemptSnapshotRepository,
            MockExamSectionRepository sections,
            JsonMapper json) {
        this.questionRepository = questionRepository;
        this.questionItemRepository = questionItemRepository;
        this.questionOptionRepository = questionOptionRepository;
        this.questionMediaRepository = questionMediaRepository;
        this.examAttemptSnapshotRepository = examAttemptSnapshotRepository;
        this.sections = sections;
        this.json = json;
    }

    ExamAttemptSnapshotEntity require(Integer attemptId) {
        return examAttemptSnapshotRepository
                .findByAttemptId(attemptId)
                .orElseThrow(
                        () -> new BusinessRuleException("Không tìm thấy snapshot của lượt thi"));
    }

    Integer durationMinutes(Integer attemptId, Integer fallback) {
        return examAttemptSnapshotRepository
                .findByAttemptId(attemptId)
                .map(ExamAttemptSnapshotEntity::getDurationMinutes)
                .orElse(fallback);
    }

    void updateAnswerVersion(Integer attemptId, int version) {
        examAttemptSnapshotRepository
                .findByAttemptId(attemptId)
                .ifPresent(
                        snapshot -> {
                            snapshot.setAnswerVersion(version);
                            examAttemptSnapshotRepository.save(snapshot);
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
        Map<Integer, SectionWindow> windows = new HashMap<>();
        List<MockExamSectionEntity> examSections = sections.findByMockExamIdOrderById(exam.getId());
        if (examSections.size() > 1) {
            examSections =
                    examSections.stream()
                            .sorted(Comparator.comparingInt(MockExamSectionEntity::getDisplayOrder))
                            .toList();
            int minute = 0;
            for (MockExamSectionEntity section : examSections) {
                int end = Math.addExact(minute, section.getDurationMinutes());
                windows.put(section.getId(), new SectionWindow(minute, end));
                minute = end;
            }
            snapshot.setDurationMinutes(minute);
        }
        for (MockExamQuestionEntity link : links) {
            QuestionEntity question =
                    questionRepository.findById(link.getQuestionId()).orElseThrow();
            Map<String, Object> q = new LinkedHashMap<>();
            q.put("id", question.getId());
            q.put("position", link.getPosition());
            q.put("content", question.getContent());
            q.put("instruction", question.getInstruction());
            q.put("readingPassage", question.getReadingPassage());
            q.put("type", question.getQuestionType());
            q.put("skill", question.getSkill());
            SectionWindow window = windows.get(link.getMockExamSectionId());
            if (window != null) {
                q.put("sectionStartMinute", window.start());
                q.put("sectionEndMinute", window.end());
            }
            List<Map<String, Object>> lockedItems = new ArrayList<>();
            for (QuestionItemEntity item :
                    questionItemRepository.findByQuestionIdOrderByPosition(question.getId())) {
                Map<String, Object> i = new LinkedHashMap<>();
                i.put("id", item.getId());
                i.put("position", item.getPosition());
                i.put("content", item.getContent());
                i.put("correctPosition", item.getCorrectPosition());
                i.put("minWordCount", item.getMinWordCount());
                i.put("maxWordCount", item.getMaxWordCount());
                i.put("responseSeconds", item.getResponseSeconds());
                List<Map<String, Object>> lockedOptions = new ArrayList<>();
                questionOptionRepository
                        .findByQuestionItemIdOrderByPosition(item.getId())
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
                    questionMediaRepository.findByQuestionIdOrderByPosition(question.getId())) {
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
        examAttemptSnapshotRepository.save(snapshot);
    }

    record SectionWindow(int start, int end) {
        static SectionWindow of(JsonNode question) {
            return question.has("sectionEndMinute")
                    ? new SectionWindow(
                            question.path("sectionStartMinute").asInt(),
                            question.path("sectionEndMinute").asInt())
                    : null;
        }

        boolean contains(LocalDateTime started, LocalDateTime now) {
            return !now.isBefore(started.plusMinutes(start))
                    && now.isBefore(started.plusMinutes(end));
        }
    }

    static void requireOpenSection(ExamAttemptEntity attempt, JsonNode question) {
        if (question == null) return;
        SectionWindow window = SectionWindow.of(question);
        if (window != null && !window.contains(attempt.getStartedAt(), LocalDateTime.now()))
            throw new BusinessRuleException("Câu hỏi không thuộc phần thi đang mở");
    }

    Map<Integer, JsonNode> lockedQuestions(Integer attemptId) {
        ExamAttemptSnapshotEntity snapshot =
                examAttemptSnapshotRepository
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

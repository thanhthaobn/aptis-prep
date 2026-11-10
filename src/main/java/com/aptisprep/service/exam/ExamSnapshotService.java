package com.aptisprep.service.exam;

import com.aptisprep.entity.*;
import com.aptisprep.mvc.common.exception.BusinessRuleException;
import com.aptisprep.repository.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;

/** Owns locked exam content and per-attempt metadata within the caller's transaction. */
@Service
public final class ExamSnapshotService {
    private final QuestionRepository questionRepository;
    private final QuestionItemRepository questionItemRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final QuestionMediaRepository questionMediaRepository;
    private final ExamAttemptSnapshotRepository examAttemptSnapshotRepository;
    private final MockExamSectionRepository sections;
    private final UserAnswerRepository answers;

    ExamSnapshotService(
            QuestionRepository questionRepository,
            QuestionItemRepository questionItemRepository,
            QuestionOptionRepository questionOptionRepository,
            QuestionMediaRepository questionMediaRepository,
            ExamAttemptSnapshotRepository examAttemptSnapshotRepository,
            MockExamSectionRepository sections,
            UserAnswerRepository answers) {
        this.questionRepository = questionRepository;
        this.questionItemRepository = questionItemRepository;
        this.questionOptionRepository = questionOptionRepository;
        this.questionMediaRepository = questionMediaRepository;
        this.examAttemptSnapshotRepository = examAttemptSnapshotRepository;
        this.sections = sections;
        this.answers = answers;
    }

    ExamAttemptSnapshotEntity require(Integer attemptId) {
        return examAttemptSnapshotRepository
                .findByAttemptId(attemptId)
                .orElseThrow(
                        () -> new BusinessRuleException("Không tìm thấy snapshot của lượt thi"));
    }

    public Optional<MockExamEntity> frozenExam(Integer attemptId) {
        return examAttemptSnapshotRepository
                .findByAttemptId(attemptId)
                .map(MockExamEntity::fromSnapshot);
    }

    public void preserveLegacyAttempt(
            ExamAttemptEntity attempt, MockExamEntity exam, List<MockExamQuestionEntity> links) {
        Set<Integer> questionIds = new HashSet<>();
        links.forEach(link -> questionIds.add(link.getQuestionId()));
        if (links.isEmpty()
                || answers.findByAttemptIdOrderByQuestionIdAsc(attempt.getId()).stream()
                        .anyMatch(answer -> !questionIds.contains(answer.getQuestionId()))) {
            throw new BusinessRuleException(
                    "Không thể khôi phục câu hỏi của lượt thi #" + attempt.getId());
        }
        createSnapshot(attempt, exam, links);
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
        Integer schemeId =
                attempt.getScoringSchemeId() == null
                        ? exam.getScoringSchemeId()
                        : attempt.getScoringSchemeId();
        snapshot.setScoringSchemeId(schemeId);
        snapshot.setExamType(exam.getExamType());
        snapshot.setSkill(exam.getSkill());
        snapshot.setTargetLevel(exam.getTargetLevel());
        snapshot.setTitle(exam.getTitle());
        snapshot.setDurationMinutes(exam.getDurationMinutes());
        snapshot.setAnswerVersion(Objects.requireNonNullElse(attempt.getAnswerVersion(), 1));
        snapshot.setPolicyVersion(
                Objects.requireNonNullElse(
                        attempt.getPolicyVersion(),
                        schemeId == null ? "unscaled-v1" : "scoring-scheme:" + schemeId + ":v1"));
        Map<String, Object> grading = new LinkedHashMap<>();
        grading.put("certificateId", exam.getCertificateId());
        grading.put("scoringSchemeId", schemeId);
        grading.put("policyVersion", snapshot.getPolicyVersion());
        grading.put("answerVersion", snapshot.getAnswerVersion());
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
            ExamQuestionSnapshotEntity questionSnapshot = new ExamQuestionSnapshotEntity();
            questionSnapshot.setSourceQuestionId(question.getId());
            questionSnapshot.setPosition(link.getPosition());
            questionSnapshot.setContent(question.getContent());
            questionSnapshot.setInstruction(question.getInstruction());
            questionSnapshot.setReadingPassage(question.getReadingPassage());
            questionSnapshot.setQuestionType(question.getQuestionType());
            questionSnapshot.setSkill(question.getSkill());
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
                questionSnapshot.setSectionStartMinute(window.start());
                questionSnapshot.setSectionEndMinute(window.end());
            }
            List<Map<String, Object>> lockedItems = new ArrayList<>();
            for (QuestionItemEntity item :
                    questionItemRepository.findByQuestionIdOrderByPosition(question.getId())) {
                ExamItemSnapshotEntity itemSnapshot = new ExamItemSnapshotEntity();
                itemSnapshot.setSourceItemId(item.getId());
                itemSnapshot.setPosition(item.getPosition());
                itemSnapshot.setContent(item.getContent());
                itemSnapshot.setCorrectPosition(item.getCorrectPosition());
                itemSnapshot.setMinWordCount(item.getMinWordCount());
                itemSnapshot.setMaxWordCount(item.getMaxWordCount());
                itemSnapshot.setResponseSeconds(item.getResponseSeconds());
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
                                    ExamOptionSnapshotEntity optionSnapshot =
                                            new ExamOptionSnapshotEntity();
                                    optionSnapshot.setSourceOptionId(option.getId());
                                    optionSnapshot.setOptionCode(option.getOptionCode());
                                    optionSnapshot.setPosition(option.getPosition());
                                    optionSnapshot.setContent(option.getContent());
                                    optionSnapshot.setCorrect(option.getCorrect());
                                    itemSnapshot.addOption(optionSnapshot);
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
                questionSnapshot.addItem(itemSnapshot);
            }
            List<Map<String, Object>> lockedMedia = new ArrayList<>();
            for (QuestionMediaEntity media :
                    questionMediaRepository.findByQuestionIdOrderByPosition(question.getId())) {
                ExamMediaSnapshotEntity mediaSnapshot = new ExamMediaSnapshotEntity();
                mediaSnapshot.setSourceMediaId(media.getId());
                mediaSnapshot.setPosition(media.getPosition());
                mediaSnapshot.setMediaType(media.getMediaType());
                mediaSnapshot.setMediaUrl(media.getMediaUrl());
                mediaSnapshot.setOriginalFilename(media.getOriginalFilename());
                mediaSnapshot.setContentType(media.getContentType());
                mediaSnapshot.setTranscript(media.getTranscript());
                mediaSnapshot.setMaxPlayCount(
                        media.getMaxPlayCount() == null ? 2 : media.getMaxPlayCount());
                questionSnapshot.addMedia(mediaSnapshot);
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
            snapshot.addQuestion(questionSnapshot);
        }
        // Snapshot content is stored in the relational child tables above.
        // Keep only a stable policy marker for compatibility with old grading events.
        examAttemptSnapshotRepository.save(snapshot);
    }

    record SectionWindow(int start, int end) {
        boolean contains(LocalDateTime started, LocalDateTime now) {
            return !now.isBefore(started.plusMinutes(start))
                    && now.isBefore(started.plusMinutes(end));
        }
    }

    LocalDateTime sectionClockStart(ExamAttemptEntity attempt) {
        long advanceMillis =
                examAttemptSnapshotRepository
                        .findByAttemptId(attempt.getId())
                        .map(ExamAttemptSnapshotEntity::getSectionAdvanceMillis)
                        .map(value -> value == null ? 0L : value)
                        .orElse(0L);
        return attempt.getStartedAt().minus(Duration.ofMillis(advanceMillis));
    }

    void advanceSectionClock(Integer attemptId, Duration remaining) {
        ExamAttemptSnapshotEntity snapshot = require(attemptId);
        // Skip unused time without changing the durations stored in the locked section windows.
        long millis = (remaining.toNanos() + 999_999) / 1_000_000;
        snapshot.setSectionAdvanceMillis(snapshot.getSectionAdvanceMillis() + millis);
        examAttemptSnapshotRepository.save(snapshot);
    }

    List<ExamQuestionSnapshotEntity> lockedQuestionEntities(Integer attemptId) {
        ExamAttemptSnapshotEntity snapshot = require(attemptId);
        return snapshot.getQuestions().stream()
                .sorted(Comparator.comparingInt(ExamQuestionSnapshotEntity::getPosition))
                .toList();
    }

    ExamQuestionSnapshotEntity lockedQuestionEntity(Integer attemptId, Integer questionId) {
        return lockedQuestionEntities(attemptId).stream()
                .filter(question -> question.getSourceQuestionId().equals(questionId))
                .findFirst()
                .orElse(null);
    }

    ExamMediaSnapshotEntity lockedMediaEntity(Integer attemptId, Integer mediaId) {
        return lockedQuestionEntities(attemptId).stream()
                .flatMap(question -> question.getMedia().stream())
                .filter(media -> media.getSourceMediaId().equals(mediaId))
                .findFirst()
                .orElse(null);
    }

    void requireOpenSection(ExamAttemptEntity attempt, ExamQuestionSnapshotEntity question) {
        if (question == null || question.getSectionEndMinute() == null) return;
        SectionWindow window =
                new SectionWindow(question.getSectionStartMinute(), question.getSectionEndMinute());
        if (!window.contains(sectionClockStart(attempt), LocalDateTime.now()))
            throw new BusinessRuleException("Câu hỏi không thuộc phần thi đang mở");
    }
}

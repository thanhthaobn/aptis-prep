package com.aptisprep.mvc.service.exam.impl;

import com.aptisprep.mvc.service.exam.*;
import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.service.exam.ExamSnapshotService;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.repository.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quản lý bản sao bất biến của đề được dùng trong một lượt thi.
 *
 * <p>Các bảng đề/câu hỏi hiện tại có thể bị chỉnh sửa sau khi học viên bắt đầu. Luồng resume và
 * chấm điểm vẫn phải thấy đúng nội dung ban đầu, nên service này copy câu hỏi, item, option,
 * media, metadata chấm và các mốc thời gian section vào bảng snapshot lúc bắt đầu.
 */
@Service
public class ExamSnapshotServiceImpl implements ExamSnapshotService {
    private QuestionRepository questionRepository;
    private QuestionItemRepository questionItemRepository;
    private QuestionOptionRepository questionOptionRepository;
    private QuestionMediaRepository questionMediaRepository;
    private ExamAttemptSnapshotRepository examAttemptSnapshotRepository;
    private MockExamSectionRepository sections;
    private UserAnswerRepository answers;

    public ExamSnapshotServiceImpl(
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

    public ExamAttemptSnapshot require(Integer attemptId) {
        return examAttemptSnapshotRepository
                .findByAttemptId(attemptId)
                .orElseThrow(
                        () -> new BusinessRuleException("Không tìm thấy snapshot của lượt thi"));
    }

    public Optional<MockExam> frozenExam(Integer attemptId) {
        return examAttemptSnapshotRepository
                .findByAttemptId(attemptId)
                .map(streamValue -> MockExam.fromSnapshot(streamValue));
    }

    @Transactional
    public void preserveLegacyAttempt(
            ExamAttempt attempt, MockExam exam, List<MockExamQuestion> links) {
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

    public Integer durationMinutes(Integer attemptId, Integer fallback) {
        return examAttemptSnapshotRepository
                .findByAttemptId(attemptId)
                .map(streamValue -> streamValue.getDurationMinutes())
                .orElse(fallback);
    }

    @Transactional
    public void updateAnswerVersion(Integer attemptId, int version) {
        examAttemptSnapshotRepository
                .findByAttemptId(attemptId)
                .ifPresent(
                        snapshot -> {
                            snapshot.setAnswerVersion(version);
                            examAttemptSnapshotRepository.save(snapshot);
                        });
    }

    @Transactional
    public void createSnapshot(
            ExamAttempt attempt, MockExam exam, List<MockExamQuestion> links) {
        // Snapshot cả version scoring/rubric cùng nội dung; nếu không, policy mới của admin có thể
        // tính lại attempt cũ bằng rule khác.
        ExamAttemptSnapshot snapshot = new ExamAttemptSnapshot();
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
        List<MockExamSection> examSections = sections.findByMockExamIdOrderById(exam.getId());
        if (examSections.size() > 1) {
            // Đề nhiều section dùng một đồng hồ liên tục ghép từ các window. Khóa mốc ở đây để
            // client không thể thay đổi thời lượng section sau đó.
            examSections =
                    examSections.stream()
                            .sorted(Comparator.comparingInt(streamValue -> streamValue.getDisplayOrder()))
                            .toList();
            int minute = 0;
            for (MockExamSection section : examSections) {
                int end = Math.addExact(minute, section.getDurationMinutes());
                windows.put(section.getId(), new SectionWindow(minute, end));
                minute = end;
            }
            snapshot.setDurationMinutes(minute);
        }
        for (MockExamQuestion link : links) {
            Question question =
                    questionRepository.findById(link.getQuestionId()).orElseThrow();
            ExamQuestionSnapshot questionSnapshot = new ExamQuestionSnapshot();
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
            for (QuestionItem item :
                    questionItemRepository.findByQuestionIdOrderByPosition(question.getId())) {
                ExamItemSnapshot itemSnapshot = new ExamItemSnapshot();
                itemSnapshot.setSourceItemId(item.getId());
                itemSnapshot.setPosition(item.getPosition());
                itemSnapshot.setPoints(item.getPoints());
                itemSnapshot.setContent(item.getContent());
                itemSnapshot.setCorrectPosition(item.getCorrectPosition());
                itemSnapshot.setMinWordCount(item.getMinWordCount());
                itemSnapshot.setMaxWordCount(item.getMaxWordCount());
                itemSnapshot.setResponseSeconds(item.getResponseSeconds());
                Map<String, Object> i = new LinkedHashMap<>();
                i.put("id", item.getId());
                i.put("position", item.getPosition());
                i.put("points", item.getPoints());
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
                                    ExamOptionSnapshot optionSnapshot =
                                            new ExamOptionSnapshot();
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
            for (QuestionMedia media :
                    questionMediaRepository.findByQuestionIdOrderByPosition(question.getId())) {
                ExamMediaSnapshot mediaSnapshot = new ExamMediaSnapshot();
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
        // Nội dung snapshot được lưu ở các bảng con quan hệ phía trên.
        // Chỉ giữ một marker policy ổn định để tương thích với các event chấm cũ.
        examAttemptSnapshotRepository.save(snapshot);
    }


    public LocalDateTime sectionClockStart(ExamAttempt attempt) {
        long advanceMillis =
                examAttemptSnapshotRepository
                        .findByAttemptId(attempt.getId())
                        .map(streamValue -> streamValue.getSectionAdvanceMillis())
                        .map(value -> value == null ? 0L : value)
                        .orElse(0L);
        return attempt.getStartedAt().minus(Duration.ofMillis(advanceMillis));
    }

    @Transactional
    public void advanceSectionClock(Integer attemptId, Duration remaining) {
        ExamAttemptSnapshot snapshot = require(attemptId);
        // Bỏ qua thời gian chưa dùng mà không thay đổi thời lượng đã lưu trong các window section bị khóa.
        long millis = (remaining.toNanos() + 999_999) / 1_000_000;
        snapshot.setSectionAdvanceMillis(snapshot.getSectionAdvanceMillis() + millis);
        examAttemptSnapshotRepository.save(snapshot);
    }

    public List<ExamQuestionSnapshot> lockedQuestionEntities(Integer attemptId) {
        ExamAttemptSnapshot snapshot = require(attemptId);
        return snapshot.getQuestions().stream()
                .sorted(Comparator.comparingInt(streamValue -> streamValue.getPosition()))
                .toList();
    }

    public ExamQuestionSnapshot lockedQuestionEntity(Integer attemptId, Integer questionId) {
        return lockedQuestionEntities(attemptId).stream()
                .filter(question -> question.getSourceQuestionId().equals(questionId))
                .findFirst()
                .orElse(null);
    }

    public ExamMediaSnapshot lockedMediaEntity(Integer attemptId, Integer mediaId) {
        return lockedQuestionEntities(attemptId).stream()
                .flatMap(question -> question.getMedia().stream())
                .filter(media -> media.getSourceMediaId().equals(mediaId))
                .findFirst()
                .orElse(null);
    }

    public void requireOpenSection(ExamAttempt attempt, ExamQuestionSnapshot question) {
        if (question == null || question.getSectionEndMinute() == null) return;
        SectionWindow window =
                new SectionWindow(question.getSectionStartMinute(), question.getSectionEndMinute());
        if (!window.contains(sectionClockStart(attempt), LocalDateTime.now()))
            throw new BusinessRuleException("Câu hỏi không thuộc phần thi đang mở");
    }
}


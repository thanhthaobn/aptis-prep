package com.aptisprep.mvc.service.exam.impl;
import com.aptisprep.mvc.support.scoring.*;
import com.aptisprep.mvc.support.contract.*;

import com.aptisprep.mvc.service.exam.AttemptResultReader;
import com.aptisprep.mvc.service.exam.ExamSnapshotService;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.Role;
import com.aptisprep.mvc.enums.UserStatus;
import com.aptisprep.mvc.exception.*;
import com.aptisprep.mvc.repository.*;
import com.aptisprep.mvc.support.contract.IntegrationModels.*;
import com.aptisprep.mvc.support.scoring.CheckpointScorePolicy;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

/** Chỉ phụ trách query/read-model; xử lý hết hạn của học viên nằm ở attempt command service. */
@Service
public class AttemptResultReaderImpl implements AttemptResultReader {
    private ExamAttemptRepository examAttemptRepository;
    private MockExamRepository mockExamRepository;
    private UserRepository userRepository;
    private UserAnswerRepository userAnswerRepository;
    private QuestionRepository questionRepository;
    private ExamSkillResultRepository examSkillResultRepository;
    private ExamSnapshotService examSnapshots;

    public AttemptResultReaderImpl(
            ExamAttemptRepository examAttemptRepository,
            MockExamRepository mockExamRepository,
            UserRepository userRepository,
            UserAnswerRepository userAnswerRepository,
            QuestionRepository questionRepository,
            ExamSkillResultRepository examSkillResultRepository,
            ExamSnapshotService examSnapshots) {
        this.examAttemptRepository = examAttemptRepository;
        this.mockExamRepository = mockExamRepository;
        this.userRepository = userRepository;
        this.userAnswerRepository = userAnswerRepository;
        this.questionRepository = questionRepository;
        this.examSkillResultRepository = examSkillResultRepository;
        this.examSnapshots = examSnapshots;
    }

    @Transactional(readOnly = true)
    public HistoryPage history(Integer userId, int page, int size, HistoryFilter filter) {
                // Lịch sử cũng được hiển thị ở màn hình chi tiết học viên của admin,
                // bao gồm cả học viên bị khóa/ngừng hoạt động. Endpoint phía học viên
                // đã kiểm tra principal đang hoạt động trước khi gọi contract này.
        int safeSize = Math.min(100, Math.max(1, size));
        int safePage = Math.min(Math.max(0, page), Integer.MAX_VALUE / safeSize);
        HistoryFilter safeFilter = filter == null ? HistoryFilter.empty() : filter;
        String pattern =
                safeFilter.query() == null || safeFilter.query().isBlank()
                        ? ""
                        : "%" + safeFilter.query().trim().toLowerCase(Locale.ROOT) + "%";
        String skill = normalizedFilter(safeFilter.skill());
        String examType = normalizedFilter(safeFilter.examType());
        String state = normalizedFilter(safeFilter.state());
        Page<ExamAttempt> result =
                examAttemptRepository.searchHistory(
                        userId,
                        pattern,
                        skill,
                        examType,
                        state,
                        PageRequest.of(safePage, safeSize));
        if (result.isEmpty() && result.getTotalElements() > 0 && safePage > 0) {
            safePage = (int) ((result.getTotalElements() - 1) / safeSize);
            result =
                    examAttemptRepository.searchHistory(
                            userId,
                            pattern,
                            skill,
                            examType,
                            state,
                            PageRequest.of(safePage, safeSize));
        }
        List<AttemptResult> values = result.stream().map(a -> view(a, examFor(a))).toList();
        return new HistoryPage(values, safePage, safeSize, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AdminHistoryPage adminHistory(
            String adminEmail, int page, int size, AdminHistoryFilter filter) {
        requireAdmin(adminEmail);
        int safePage = Math.max(0, page), safeSize = Math.min(100, Math.max(1, size));
        AdminHistoryFilter f =
                filter == null ? new AdminHistoryFilter("", "", "", "", null, null) : filter;
        String pattern =
                f.query() == null || f.query().isBlank()
                        ? ""
                        : "%" + f.query().trim().toLowerCase(Locale.ROOT) + "%";
        LocalDateTime from =
                f.submittedFrom() == null
                        ? null
                        : LocalDateTime.ofInstant(f.submittedFrom(), ZoneOffset.UTC);
        LocalDateTime to =
                f.submittedTo() == null
                        ? null
                        : LocalDateTime.ofInstant(f.submittedTo(), ZoneOffset.UTC);
        Page<ExamAttempt> pageData =
                examAttemptRepository.searchAllHistory(
                        pattern,
                        normalizedFilter(f.skill()),
                        normalizedFilter(f.examType()),
                        normalizedFilter(f.state()),
                        from,
                        to,
                        PageRequest.of(safePage, safeSize));
        List<AdminHistoryItem> values =
                pageData.stream()
                        .map(
                                a -> {
                                    User user =
                                            userRepository.findById(a.getUserId()).orElseThrow();
                                    return new AdminHistoryItem(
                                            user.getFullName(),
                                            user.getEmail(),
                                            view(a, examFor(a)));
                                })
                        .toList();
        return new AdminHistoryPage(values, safePage, safeSize, pageData.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AttemptResult adminResult(String adminEmail, Integer attemptId) {
        requireAdmin(adminEmail);
        ExamAttempt attempt =
                examAttemptRepository
                        .findById(attemptId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kết quả"));
        return view(attempt, examFor(attempt));
    }

    public AttemptResult view(ExamAttempt attempt, MockExam exam) {
        List<SkillResult> skills =
                examSkillResultRepository.findByAttemptIdOrderBySkillAsc(attempt.getId()).stream()
                        .map(
                                value -> {
                                    String confirmedLevel =
                                            CheckpointScorePolicy.confirmedLevel(
                                                    exam.getExamType(),
                                                    exam.getTargetLevel(),
                                                    value.getCefrLevel());
                                    String state = value.getGradingStatus();
                                    if (state == null)
                                        state =
                                                value.getScaleScore() == null
                                                        ? "PENDING"
                                                        : "COMPLETED";
                                    else if ("WAITING".equals(state)) {
                                        // Cờ WAITING đã cũ không được che mất điểm đã lưu.
                                        // Tình huống này có thể xảy ra khi objective hoàn tất trước
                                        // khi callback chấm bất đồng bộ cập nhật aggregate của attempt.
                                        state =
                                                value.getRawScore() != null
                                                                && value.getScaleScore() != null
                                                        ? "COMPLETED"
                                                        : "PENDING";
                                    } else if ("ERROR".equals(state)) state = "ERROR";
                                    boolean passed =
                                            "COMPLETED".equals(state)
                                                    && CheckpointScorePolicy.passed(
                                                            exam.getExamType(),
                                                            exam.getTargetLevel(),
                                                            confirmedLevel,
                                                            value.getScaleScore());
                                    String feedback =
                                            feedbackFor(attempt.getId(), value.getSkill());
                                    if ("ERROR".equals(state)
                                            && "AI_ERROR".equals(value.getGradingMethod())
                                            && "SPEAKING".equals(value.getSkill()))
                                        feedback = "Không nhận được bản chép lời từ file ghi âm.";
                                    return new SkillResult(
                                            value.getSkill(),
                                            value.getRawScore(),
                                            value.getRawMax(),
                                            value.getScaleScore(),
                                            confirmedLevel,
                                            state,
                                            feedback,
                                            passed,
                                            "ERROR".equals(state)
                                                    && ("AI_ERROR".equals(value.getGradingMethod())
                                                            || "OBJECTIVE_ERROR"
                                                                    .equals(
                                                                            value
                                                                                    .getGradingMethod())));
                                })
                        .toList();
        Integer schemeId =
                attempt.getScoringSchemeId() == null
                        ? exam.getScoringSchemeId()
                        : attempt.getScoringSchemeId();
        return new AttemptResult(
                attempt.getId(),
                attempt.getUserId(),
                exam.getId(),
                exam.getExamType(),
                exam.getTitle(),
                schemeId,
                attempt.getStatus(),
                skills,
                attempt.getFinalScaleScore(),
                attempt.getEstimatedOverallCefr(),
                instant(attempt.getSubmittedAt()));
    }

    private String feedbackFor(Integer attemptId, String skill) {
        StringJoiner feedback = new StringJoiner("\n\n");
        for (UserAnswer answer :
                userAnswerRepository.findByAttemptIdOrderByQuestionIdAsc(attemptId)) {
            if (answer.getAiFeedback() == null || answer.getAiFeedback().isBlank()) continue;
            Question question =
                    questionRepository.findById(answer.getQuestionId()).orElse(null);
            if (question == null || !Objects.equals(question.getSkill(), skill)) continue;
            int questionNumber = questionNumber(attemptId, question.getId());
            String label = questionNumber > 0 ? "Câu hỏi " + questionNumber : "Câu hỏi";
            feedback.add(label + ":\n" + answer.getAiFeedback().trim());
        }
        return feedback.length() == 0 ? null : feedback.toString();
    }

    private int questionNumber(Integer attemptId, Integer sourceQuestionId) {
        try {
            return examSnapshots.lockedQuestionEntities(attemptId).stream()
                    .filter(value -> Objects.equals(value.getSourceQuestionId(), sourceQuestionId))
                    .map(streamValue -> streamValue.getPosition())
                    .filter(streamValue -> Objects.nonNull(streamValue))
                    .findFirst()
                    .orElse(0);
        } catch (BusinessRuleException legacyAttempt) {
            return 0;
        }
    }

    private User requireAdmin(String email) {
        User user =
                userRepository
                        .findByEmailIgnoreCase(email)
                        .orElseThrow(() -> new AccessDeniedException("Không có quyền quản trị"));
        if (user.getRole() != Role.ADMIN || user.getStatus() != UserStatus.ACTIVE)
            throw new AccessDeniedException("Không có quyền quản trị");
        return user;
    }

    private String normalizedFilter(String value) {
        return value == null || value.isBlank() ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private MockExam examFor(ExamAttempt attempt) {
        return examSnapshots
                .frozenExam(attempt.getId())
                .orElseGet(
                        () ->
                                mockExamRepository
                                        .findById(attempt.getMockExamId())
                                        .orElseThrow(
                                                () ->
                                                        new ResourceNotFoundException(
                                                                "Không tìm thấy đề thi")));
    }

    private Instant instant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }
}








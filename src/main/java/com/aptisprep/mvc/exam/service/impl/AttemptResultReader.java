package com.aptisprep.mvc.exam.service.impl;

import com.aptisprep.mvc.auth.entity.User;
import com.aptisprep.mvc.auth.service.UserService;
import com.aptisprep.mvc.catalog.entity.*;
import com.aptisprep.mvc.catalog.service.ExamCatalogService;
import com.aptisprep.mvc.common.enums.Role;
import com.aptisprep.mvc.common.enums.UserStatus;
import com.aptisprep.mvc.common.exception.*;
import com.aptisprep.mvc.exam.entity.*;
import com.aptisprep.mvc.exam.repository.ExamAttemptRepository;
import com.aptisprep.mvc.exam.repository.UserAnswerRepository;
import com.aptisprep.mvc.grading.service.GradingResultQueryService;
import com.aptisprep.mvc.scoring.service.impl.CheckpointScorePolicy;
import com.aptisprep.service.contract.IntegrationModels.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Query/read-model responsibility only; student expiration stays in the attempt command service. */
@Component
class AttemptResultReader {
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamCatalogService examCatalogService;
    private final UserService userService;
    private final UserAnswerRepository userAnswerRepository;
    private final GradingResultQueryService gradingResultQueryService;
    private final ExamSnapshotFactory examSnapshots;

    AttemptResultReader(
            ExamAttemptRepository examAttemptRepository,
            ExamCatalogService examCatalogService,
            UserService userService,
            UserAnswerRepository userAnswerRepository,
            GradingResultQueryService gradingResultQueryService,
            ExamSnapshotFactory examSnapshots) {
        this.examAttemptRepository = examAttemptRepository;
        this.examCatalogService = examCatalogService;
        this.userService = userService;
        this.userAnswerRepository = userAnswerRepository;
        this.gradingResultQueryService = gradingResultQueryService;
        this.examSnapshots = examSnapshots;
    }

    @Transactional(readOnly = true)
    public HistoryPage history(Integer userId, int page, int size, HistoryFilter filter) {
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
        Page<ExamAttemptEntity> result =
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
        Page<ExamAttemptEntity> pageData =
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
                                    User user = userService.findById(a.getUserId()).orElseThrow();
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
        ExamAttemptEntity attempt =
                examAttemptRepository
                        .findById(attemptId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kết quả"));
        return view(attempt, examFor(attempt));
    }

    AttemptResult view(ExamAttemptEntity attempt, MockExamEntity exam) {
        List<SkillResult> skills =
                gradingResultQueryService.findSkillResultsByAttemptId(attempt.getId()).stream()
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
        for (UserAnswerEntity answer :
                userAnswerRepository.findByAttemptIdOrderByQuestionIdAsc(attemptId)) {
            if (answer.getAiFeedback() == null || answer.getAiFeedback().isBlank()) continue;
            QuestionEntity question =
                    examCatalogService.findQuestionById(answer.getQuestionId()).orElse(null);
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
                    .map(ExamQuestionSnapshotEntity::getPosition)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(0);
        } catch (BusinessRuleException legacyAttempt) {
            return 0;
        }
    }

    private User requireAdmin(String email) {
        User user =
                userService
                        .findByEmailIgnoreCase(email)
                        .orElseThrow(() -> new AccessDeniedException("Không có quyền quản trị"));
        if (user.getRole() != Role.ADMIN || user.getStatus() != UserStatus.ACTIVE)
            throw new AccessDeniedException("Không có quyền quản trị");
        return user;
    }

    private String normalizedFilter(String value) {
        return value == null || value.isBlank() ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private MockExamEntity examFor(ExamAttemptEntity attempt) {
        return examSnapshots
                .frozenExam(attempt.getId())
                .orElseGet(
                        () ->
                                examCatalogService
                                        .findMockExamById(attempt.getMockExamId())
                                        .orElseThrow(
                                                () ->
                                                        new ResourceNotFoundException(
                                                                "Không tìm thấy đề thi")));
    }

    private Instant instant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }
}

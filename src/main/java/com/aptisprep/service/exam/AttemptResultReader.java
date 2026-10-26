package com.aptisprep.service.exam;

import com.aptisprep.entity.*;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.exception.*;
import com.aptisprep.repository.*;
import com.aptisprep.service.contract.IntegrationModels.*;
import com.aptisprep.service.scoring.CheckpointScorePolicy;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Query/read-model responsibility only; student expiration stays in the attempt command service. */
@Service
public class AttemptResultReader {
    private final ExamAttemptRepository attempts;
    private final MockExamRepository exams;
    private final UserRepository users;
    private final UserAnswerRepository answers;
    private final QuestionRepository questions;
    private final ExamSkillResultRepository skillResults;

    public AttemptResultReader(
            ExamAttemptRepository attempts,
            MockExamRepository exams,
            UserRepository users,
            UserAnswerRepository answers,
            QuestionRepository questions,
            ExamSkillResultRepository skillResults) {
        this.attempts = attempts;
        this.exams = exams;
        this.users = users;
        this.answers = answers;
        this.questions = questions;
        this.skillResults = skillResults;
    }

    @Transactional(readOnly = true)
    public HistoryPage history(Integer userId, int page, int size, HistoryFilter filter) {
        // History is also rendered by the admin's student-detail view, including
        // locked/inactive students. Student-facing endpoints validate the active
        // principal before calling this contract.
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
        var result =
                attempts.searchHistory(
                        userId,
                        pattern,
                        skill,
                        examType,
                        state,
                        PageRequest.of(safePage, safeSize));
        if (result.isEmpty() && result.getTotalElements() > 0 && safePage > 0) {
            safePage = (int) ((result.getTotalElements() - 1) / safeSize);
            result =
                    attempts.searchHistory(
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
        var pageData =
                attempts.searchAllHistory(
                        pattern,
                        normalizedFilter(f.skill()),
                        normalizedFilter(f.examType()),
                        normalizedFilter(f.state()),
                        from,
                        to,
                        PageRequest.of(safePage, safeSize));
        var values =
                pageData.stream()
                        .map(
                                a -> {
                                    var user = users.findById(a.getUserId()).orElseThrow();
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
        var attempt =
                attempts.findById(attemptId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kết quả"));
        return view(attempt, examFor(attempt));
    }

    AttemptResult view(ExamAttemptEntity attempt, MockExamEntity exam) {
        List<SkillResult> skills =
                skillResults.findByAttemptIdOrderBySkillAsc(attempt.getId()).stream()
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
                                    else if ("WAITING".equals(state)) state = "PENDING";
                                    else if ("ERROR".equals(state)) state = "ERROR";
                                    boolean passed =
                                            "COMPLETED".equals(state)
                                                    && CheckpointScorePolicy.passed(
                                                            exam.getExamType(),
                                                            exam.getTargetLevel(),
                                                            confirmedLevel,
                                                            value.getScaleScore());
                                    return new SkillResult(
                                            value.getSkill(),
                                            value.getRawScore(),
                                            value.getScaleScore(),
                                            confirmedLevel,
                                            state,
                                            feedbackFor(attempt.getId(), value.getSkill()),
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
        for (UserAnswerEntity answer : answers.findByAttemptIdOrderByQuestionIdAsc(attemptId)) {
            if (answer.getAiFeedback() == null || answer.getAiFeedback().isBlank()) continue;
            QuestionEntity question = questions.findById(answer.getQuestionId()).orElse(null);
            if (question != null && Objects.equals(question.getSkill(), skill))
                feedback.add(answer.getAiFeedback().trim());
        }
        return feedback.length() == 0 ? null : feedback.toString();
    }

    private User requireAdmin(String email) {
        User user =
                users.findByEmailIgnoreCase(email)
                        .orElseThrow(() -> new AccessDeniedException("Không có quyền quản trị"));
        if (user.getRole() != Role.ADMIN || user.getStatus() != UserStatus.ACTIVE)
            throw new AccessDeniedException("Không có quyền quản trị");
        return user;
    }

    private String normalizedFilter(String value) {
        return value == null || value.isBlank() ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private MockExamEntity examFor(ExamAttemptEntity attempt) {
        return exams.findById(attempt.getMockExamId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));
    }

    private Instant instant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }
}

package com.aptisprep.mvc.service.exam.impl;

import com.aptisprep.mvc.entity.ExamAttempt;
import com.aptisprep.mvc.entity.MockExam;
import com.aptisprep.mvc.entity.User;
import com.aptisprep.mvc.enums.Role;
import com.aptisprep.mvc.enums.UserStatus;
import com.aptisprep.mvc.exception.AccessDeniedException;
import com.aptisprep.mvc.exception.ResourceNotFoundException;
import com.aptisprep.mvc.repository.ExamAttemptRepository;
import com.aptisprep.mvc.repository.MockExamRepository;
import com.aptisprep.mvc.repository.UserRepository;
import com.aptisprep.mvc.service.exam.AttemptResultReader;
import com.aptisprep.mvc.service.exam.ExamAttemptQueryService;
import com.aptisprep.mvc.service.exam.ExamSnapshotService;
import com.aptisprep.mvc.support.contract.IntegrationModels.AttemptResult;
import com.aptisprep.mvc.support.contract.IntegrationModels.AdminHistoryFilter;
import com.aptisprep.mvc.support.contract.IntegrationModels.AdminHistoryPage;
import com.aptisprep.mvc.support.contract.IntegrationModels.HistoryFilter;
import com.aptisprep.mvc.support.contract.IntegrationModels.HistoryPage;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implements attempt reads without depending on the attempt command service. */
@Service
public class ExamAttemptQueryServiceImpl implements ExamAttemptQueryService {
    private static final Pattern CHECKPOINT_REQUEST_KEY =
            Pattern.compile("checkpoint:(\\d+):(\\d+):after:\\d+");
    private static final Set<String> ACTIVE_STATES =
            Set.of("IN_PROGRESS", "SUBMITTED", "WAITING_GRADING");

    private final UserRepository userRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final MockExamRepository mockExamRepository;
    private final ExamSnapshotService examSnapshots;
    private final AttemptResultReader resultReader;

    public ExamAttemptQueryServiceImpl(
            UserRepository userRepository,
            ExamAttemptRepository examAttemptRepository,
            MockExamRepository mockExamRepository,
            ExamSnapshotService examSnapshots,
            AttemptResultReader resultReader) {
        this.userRepository = userRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.mockExamRepository = mockExamRepository;
        this.examSnapshots = examSnapshots;
        this.resultReader = resultReader;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer latestResumableAttemptId(Integer userId, Integer examId) {
        activeUser(userId);
        LocalDateTime now = LocalDateTime.now();
        return examAttemptRepository
                .findByUserIdAndMockExamIdAndStatusAndSubmittedAtIsNullOrderByIdDesc(
                        userId, examId, "IN_PROGRESS")
                .stream()
                .filter(
                        attempt -> {
                            LocalDateTime deadline = examDeadline(attempt, examFor(attempt));
                            return deadline == null || !deadline.isBefore(now);
                        })
                .map(ExamAttempt::getId)
                .findFirst()
                .orElse(null);
    }

    @Override
    @Transactional
    public AttemptResult result(Integer userId, Integer attemptId) {
        activeUser(userId);
        ExamAttempt attempt =
                examAttemptRepository
                        .findByIdAndUserIdForUpdate(attemptId, userId)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Bạn không có quyền xem kết quả này"));
        if ("CANCELLED".equals(attempt.getStatus()))
            throw new ResourceNotFoundException("Lượt làm dở đã được xóa.");
        MockExam exam = examFor(attempt);
        expireIfNeeded(attempt, exam);
        return resultReader.view(attempt, exam);
    }

    @Override
    @Transactional(readOnly = true)
    public HistoryPage history(Integer userId, int page, int size, HistoryFilter filter) {
        return resultReader.history(userId, page, size, filter);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminHistoryPage adminHistory(
            String adminEmail, int page, int size, AdminHistoryFilter filter) {
        return resultReader.adminHistory(adminEmail, page, size, filter);
    }

    @Override
    @Transactional(readOnly = true)
    public AttemptResult adminResult(String adminEmail, Integer attemptId) {
        return resultReader.adminResult(adminEmail, attemptId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasInProgressPlacement(Integer userId, String skill) {
        return examAttemptRepository
                .findActivePlacement(userId, skill, Set.of("IN_PROGRESS"))
                .isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasInProgressPlacement(Integer userId) {
        activeUser(userId);
        return examAttemptRepository.findPlacementAttempts(userId).stream()
                .anyMatch(attempt -> ACTIVE_STATES.contains(attempt.getStatus()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPlacementAttempt(Integer userId) {
        activeUser(userId);
        return !examAttemptRepository.findPlacementAttempts(userId).isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Integer> checkpointOriginPathId(Integer userId, Integer attemptId) {
        activeUser(userId);
        return examAttemptRepository
                .findByIdAndUserId(attemptId, userId)
                .map(ExamAttempt::getRequestKey)
                .flatMap(
                        requestKey -> {
                            Matcher match = CHECKPOINT_REQUEST_KEY.matcher(requestKey);
                            return match.matches() && userId.toString().equals(match.group(1))
                                    ? Optional.of(Integer.valueOf(match.group(2)))
                                    : Optional.empty();
                        });
    }

    private User activeUser(Integer userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        if (user.getRole() != Role.STUDENT || user.getStatus() != UserStatus.ACTIVE)
            throw new AccessDeniedException("Tài khoản không còn hoạt động");
        return user;
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

    private void expireIfNeeded(ExamAttempt attempt, MockExam exam) {
        if (!"IN_PROGRESS".equals(attempt.getStatus())) return;
        Integer minutes = examSnapshots.durationMinutes(attempt.getId(), exam.getDurationMinutes());
        if (minutes == null || attempt.getStartedAt() == null) return;
        LocalDateTime deadline =
                examSnapshots.sectionClockStart(attempt).plusMinutes(minutes);
        if (!deadline.isBefore(LocalDateTime.now())) return;
        attempt.setStatus("EXPIRED");
        attempt.setGradingStatus("EXPIRED");
        examAttemptRepository.save(attempt);
    }

    private LocalDateTime examDeadline(ExamAttempt attempt, MockExam exam) {
        Integer minutes = examSnapshots.durationMinutes(attempt.getId(), exam.getDurationMinutes());
        return minutes == null || attempt.getStartedAt() == null
                ? null
                : examSnapshots.sectionClockStart(attempt).plusMinutes(minutes);
    }
}

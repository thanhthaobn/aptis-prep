package com.aptisprep.service.grading;

import com.aptisprep.entity.*;
import com.aptisprep.enums.*;
import com.aptisprep.event.AiAssessmentCompletedEvent;
import com.aptisprep.exception.AccessDeniedException;
import com.aptisprep.exception.AiGradingException;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.*;
import com.aptisprep.service.contract.AiAssessmentContract;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AiAssessmentService implements AiAssessmentContract {
    private static final Set<String> SKILLS = Set.of("WRITING", "SPEAKING");
    private static final Set<String> LEVELS = Set.of("A1", "A2", "B1", "B2", "C1", "C2");
    private static final int MAX_RETRIES = 3;
    private final AiAssessmentJobRepository jobs;
    private final AiAssessmentRepository assessments;
    private final UserRepository users;
    private final WritingGradingService writing;
    private final SpeakingGradingService speaking;
    private final TransactionTemplate transactions;
    private final ApplicationEventPublisher events;

    public AiAssessmentService(AiAssessmentJobRepository jobs, AiAssessmentRepository assessments, UserRepository users,
            WritingGradingService writing, SpeakingGradingService speaking, PlatformTransactionManager transactionManager,
            ApplicationEventPublisher events) {
        this.jobs = jobs; this.assessments = assessments; this.users = users; this.writing = writing; this.speaking = speaking;
        this.transactions = new TransactionTemplate(transactionManager); this.events = events;
    }

    @Override
    public AssessmentView request(AssessmentRequest request) {
        validateRequest(request);
        try {
            return transactions.execute(status -> {
                Optional<AiAssessmentJob> found = jobs.findByAssessmentKeyForUpdate(request.assessmentKey());
                if (found.isPresent()) {
                    AiAssessmentJob job = found.get();
                    if (!sameRequest(job, request)) throw new BusinessRuleException("assessmentKey đã được dùng cho một phiên bản bài khác");
                    return viewFor(job);
                }
                User user = activeUser(request.userId());
                AiAssessmentJob job = new AiAssessmentJob();
                job.setAssessmentKey(request.assessmentKey()); job.setUserId(user.getId()); job.setAttemptId(request.attemptId()); job.setAnswerId(request.answerId()); job.setAnswerVersion(request.answerVersion());
                job.setSkill(normalizeSkill(request.skill())); job.setTaskType(normalizeTask(request.taskType())); job.setTaskContent(request.taskContent().trim()); job.setLockedContent(request.lockedContent().trim());
                job.setRubricVersion(request.rubricVersion().trim()); job.setPolicyVersion(request.policyVersion().trim()); job.setMaxScore(request.maxScore()); job.setTranscript(blankToNull(request.transcript())); job.setState(AiJobState.PENDING); job.setRetryCount(0);
                jobs.save(job);
                AiAssessment assessment = new AiAssessment(); assessment.setJob(job); assessment.setAssessmentKey(job.getAssessmentKey()); assessment.setAnswerId(job.getAnswerId()); assessment.setAnswerVersion(job.getAnswerVersion()); assessment.setState(AiAssessmentState.PENDING); assessment.setRubricVersion(job.getRubricVersion()); assessment.setPolicyVersion(job.getPolicyVersion()); assessments.save(assessment);
                return viewFor(job);
            });
        } catch (org.springframework.dao.DataIntegrityViolationException duplicate) {
            return transactions.execute(status -> jobs.findByAssessmentKeyForUpdate(request.assessmentKey())
                    .map(job -> {
                        if (!sameRequest(job, request)) throw new BusinessRuleException("assessmentKey đã được dùng cho một phiên bản bài khác");
                        return viewFor(job);
                    })
                    .orElseThrow(() -> new BusinessRuleException("Không thể tạo tác vụ đánh giá lúc này")));
        }
    }

    @Override
    public AssessmentView process(String assessmentKey) {
        JobSnapshot claim = transactions.execute(status -> claim(assessmentKey));
        if (claim.view() != null) return claim.view();
        GradingResult result;
        try {
            result = switch (claim.taskType()) {
                case "WRITING" -> writing.grade(claim.taskContent(), claim.lockedContent());
                case "SPEAKING" -> {
                    if (claim.transcript() == null || claim.transcript().isBlank()) throw new AiGradingException("Chưa có transcript để chấm Speaking.");
                    yield speaking.gradeTranscript(claim.taskContent(), claim.transcript());
                }
                default -> throw new AiGradingException("Dạng bài tự luận không được hỗ trợ.");
            };
            validateResult(result, claim.maxScore());
        } catch (AiGradingException exception) {
            return fail(claim.assessmentKey(), cleanError(exception.getMessage()));
        } catch (RuntimeException exception) {
            return fail(claim.assessmentKey(), "Chưa thể chấm bài lúc này. Vui lòng thử lại sau.");
        }
        return succeed(claim, result);
    }

    @Override
    public AssessmentView retry(String assessmentKey, Integer requesterId, boolean admin) {
        transactions.executeWithoutResult(status -> {
            AiAssessmentJob job = job(assessmentKey);
            authorize(job, requesterId, admin);
            if (job.getState() != AiJobState.ERROR) throw new BusinessRuleException("Chỉ tác vụ lỗi mới được thử lại");
            if (job.getRetryCount() >= MAX_RETRIES) throw new BusinessRuleException("Tác vụ đã hết số lần thử lại");
            job.setState(AiJobState.PENDING); job.setLastError(null); jobs.save(job);
            assessments.findFirstByJobIdAndAnswerVersionOrderByIdDesc(job.getId(), job.getAnswerVersion()).ifPresent(value -> { value.setState(AiAssessmentState.PENDING); value.setErrorMessage(null); assessments.save(value); });
        });
        return process(assessmentKey);
    }

    @Override
    public AssessmentView get(String assessmentKey, Integer requesterId, boolean admin) {
        AiAssessmentJob job = job(assessmentKey); authorize(job, requesterId, admin); return viewFor(job);
    }

    private JobSnapshot claim(String key) {
        AiAssessmentJob job = jobs.findByAssessmentKeyForUpdate(key)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tác vụ đánh giá"));
        if (job.getState() == AiJobState.COMPLETED) return new JobSnapshot(key, null, null, null, null, null, null, null, null, null, viewFor(job));
        if (job.getState() == AiJobState.PROCESSING) return new JobSnapshot(key, null, null, null, null, null, null, null, null, null, viewFor(job));
        if (job.getRetryCount() >= MAX_RETRIES) throw new BusinessRuleException("Tác vụ đã hết số lần thử lại");
        job.setState(AiJobState.PROCESSING); job.setRetryCount(job.getRetryCount() + 1); jobs.save(job);
        return new JobSnapshot(key, job.getTaskType(), job.getTaskContent(), job.getLockedContent(), job.getTranscript(), job.getMaxScore(), job.getUserId(), job.getAttemptId(), job.getAnswerId(), job.getAnswerVersion(), null);
    }

    private AssessmentView succeed(JobSnapshot claim, GradingResult result) {
        AssessmentView view = transactions.execute(status -> {
            AiAssessmentJob job = job(claim.assessmentKey()); job.setState(AiJobState.COMPLETED); job.setLastError(null); jobs.save(job);
            AiAssessment assessment = assessments.findFirstByJobIdAndAnswerVersionOrderByIdDesc(job.getId(), job.getAnswerVersion()).orElseThrow();
            assessment.setState(AiAssessmentState.COMPLETED); assessment.setScore(result.estimatedScore()); assessment.setEstimatedCefr(result.estimatedCefr()); assessment.setFeedback(result.feedback()); assessment.setTranscript(result.transcript() == null ? job.getTranscript() : result.transcript()); assessment.setProvider(result.provider()); assessment.setModel(result.model()); assessment.setErrorMessage(null); assessment.setCompletedAt(LocalDateTime.now()); assessments.save(assessment);
            return viewFor(job);
        });
        publishAfterCommit(new AiAssessmentCompletedEvent(claim.assessmentKey(), claim.userId(), claim.attemptId(), claim.answerId()));
        return view;
    }

    private void publishAfterCommit(AiAssessmentCompletedEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            events.publishEvent(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { events.publishEvent(event); }
        });
    }

    private AssessmentView fail(String key, String error) {
        return transactions.execute(status -> {
            AiAssessmentJob job = job(key); job.setState(AiJobState.ERROR); job.setLastError(error); jobs.save(job);
            AiAssessment assessment = assessments.findFirstByJobIdAndAnswerVersionOrderByIdDesc(job.getId(), job.getAnswerVersion()).orElseThrow(); assessment.setState(AiAssessmentState.ERROR); assessment.setScore(null); assessment.setEstimatedCefr(null); assessment.setFeedback(null); assessment.setErrorMessage(error); assessment.setCompletedAt(null); assessments.save(assessment); return viewFor(job);
        });
    }

    private AssessmentView viewFor(AiAssessmentJob job) {
        AiAssessment assessment = assessments.findFirstByJobIdAndAnswerVersionOrderByIdDesc(job.getId(), job.getAnswerVersion()).orElse(null);
        if (assessment == null) return new AssessmentView(null, job.getAssessmentKey(), job.getAnswerId(), job.getAnswerVersion(), job.getState().name(), null, null, null, job.getTranscript(), job.getRubricVersion(), job.getPolicyVersion(), null, null, job.getLastError(), instant(job.getCreatedAt()), null);
        return new AssessmentView(assessment.getId(), assessment.getAssessmentKey(), assessment.getAnswerId(), assessment.getAnswerVersion(), assessment.getState().name(), assessment.getScore(), assessment.getEstimatedCefr(), assessment.getFeedback(), assessment.getTranscript(), assessment.getRubricVersion(), assessment.getPolicyVersion(), assessment.getProvider(), assessment.getModel(), assessment.getErrorMessage(), instant(assessment.getCreatedAt()), instant(assessment.getCompletedAt()));
    }

    private void validateRequest(AssessmentRequest request) {
        if (request == null || blank(request.assessmentKey()) || request.assessmentKey().length() > 255 || request.userId() == null || request.attemptId() == null || request.answerId() == null || request.answerVersion() == null || request.answerVersion() < 1) throw new BusinessRuleException("Yêu cầu đánh giá không hợp lệ");
        if (blank(request.taskContent()) || request.taskContent().length() > 10000 || blank(request.lockedContent()) || request.lockedContent().length() > 20000) throw new BusinessRuleException("Nội dung bài đã khóa không hợp lệ");
        if (blank(request.rubricVersion()) || blank(request.policyVersion()) || request.maxScore() == null || request.maxScore().compareTo(BigDecimal.ZERO) <= 0 || request.maxScore().compareTo(BigDecimal.valueOf(50)) > 0) throw new BusinessRuleException("Thiếu phiên bản rubric/policy hoặc thang điểm không hợp lệ");
        normalizeSkill(request.skill()); normalizeTask(request.taskType());
    }
    private boolean sameRequest(AiAssessmentJob job, AssessmentRequest request) {
        return Objects.equals(job.getUserId(), request.userId())
                && Objects.equals(job.getAttemptId(), request.attemptId())
                && Objects.equals(job.getAnswerId(), request.answerId())
                && Objects.equals(job.getAnswerVersion(), request.answerVersion())
                && Objects.equals(job.getSkill(), normalizeSkill(request.skill()))
                && Objects.equals(job.getTaskType(), normalizeTask(request.taskType()))
                && Objects.equals(job.getTaskContent(), request.taskContent().trim())
                && Objects.equals(job.getLockedContent(), request.lockedContent().trim())
                && Objects.equals(job.getRubricVersion(), request.rubricVersion().trim())
                && Objects.equals(job.getPolicyVersion(), request.policyVersion().trim())
                && Objects.equals(job.getMaxScore(), request.maxScore())
                && Objects.equals(job.getTranscript(), blankToNull(request.transcript()));
    }
    private User activeUser(Integer id) {
        User user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        if (user.getRole() != Role.STUDENT || user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("Tài khoản không còn hoạt động");
        }
        return user;
    }
    private AiAssessmentJob job(String key) { return jobs.findByAssessmentKey(key).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tác vụ đánh giá")); }
    private void authorize(AiAssessmentJob job, Integer requesterId, boolean admin) {
        User requester = users.findById(requesterId).orElseThrow(() -> new AccessDeniedException("Bạn không có quyền xem đánh giá này"));
        if (requester.getStatus() != UserStatus.ACTIVE
                || (admin && requester.getRole() != Role.ADMIN)
                || (!admin && (requester.getRole() != Role.STUDENT || !Objects.equals(job.getUserId(), requesterId)))) {
            throw new AccessDeniedException("Bạn không có quyền xem đánh giá này");
        }
    }
    private String normalizeSkill(String value) { String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT); if (!SKILLS.contains(normalized)) throw new BusinessRuleException("Kỹ năng AI không hợp lệ"); return normalized; }
    private String normalizeTask(String value) { String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT); if (!normalized.equals("WRITING") && !normalized.equals("SPEAKING")) throw new BusinessRuleException("Dạng bài AI không hợp lệ"); return normalized; }
    private void validateResult(GradingResult result, BigDecimal max) { if (result == null || result.estimatedScore() == null || result.estimatedScore().compareTo(BigDecimal.ZERO) < 0 || result.estimatedScore().compareTo(max) > 0 || result.estimatedCefr() == null || !LEVELS.contains(result.estimatedCefr()) || blank(result.feedback()) || result.feedback().length() > 12000) throw new AiGradingException("Chưa nhận được kết quả chấm hợp lệ. Vui lòng thử lại sau."); }
    private String cleanError(String message) { return message == null || message.isBlank() || message.length() > 1000 ? "Chưa thể chấm bài lúc này. Vui lòng thử lại sau." : message; }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String blankToNull(String value) { return blank(value) ? null : value.trim(); }
    private Instant instant(LocalDateTime value) { return value == null ? null : value.toInstant(ZoneOffset.UTC); }
    private record JobSnapshot(String assessmentKey, String taskType, String taskContent, String lockedContent, String transcript, BigDecimal maxScore, Integer userId, Integer attemptId, Integer answerId, Integer answerVersion, AssessmentView view) {}
}

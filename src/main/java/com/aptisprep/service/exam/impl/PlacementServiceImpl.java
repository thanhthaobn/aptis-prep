package com.aptisprep.service.exam.impl;

import com.aptisprep.dto.AttemptHandleDTO;
import com.aptisprep.dto.AttemptResultDTO;
import com.aptisprep.dto.PlacementDTO.PlacementExam;
import com.aptisprep.dto.PlacementDTO.PlacementOption;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.service.account.AccountAccessService;
import com.aptisprep.service.admin.ExamCatalogService;
import com.aptisprep.service.exam.ExamAttemptService;
import com.aptisprep.service.exam.PlacementExamCatalogService;
import com.aptisprep.service.exam.PlacementService;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class PlacementServiceImpl implements PlacementService {
    private final PlacementExamCatalogService catalog;
    private final ExamCatalogService examCatalog;
    private final ObjectProvider<ExamAttemptService> attempts;
    private final ObjectProvider<AccountAccessService> accounts;

    public PlacementServiceImpl(
            PlacementExamCatalogService catalog,
            ExamCatalogService examCatalog,
            ObjectProvider<ExamAttemptService> attempts,
            ObjectProvider<AccountAccessService> accounts) {
        this.catalog = catalog;
        this.examCatalog = examCatalog;
        this.attempts = attempts;
        this.accounts = accounts;
    }

    @Override
    public List<PlacementExam> available(String skill) {
        return catalog.findPublished(skill);
    }

    @Override
    public List<PlacementExam> available() {
        return catalog.findPublished();
    }

    @Override
    public List<PlacementOption> availableForStudent(String email) {
        AccountAccessService account = accounts.getIfAvailable();
        ExamAttemptService attempt = attempts.getIfAvailable();
        Integer userId = account == null ? null : account.requireActiveStudent(email);
        boolean resumable =
                userId != null && attempt != null && attempt.hasInProgressPlacement(userId);
        return available().stream()
                .map(
                        exam ->
                                new PlacementOption(
                                        exam.id(),
                                        exam.title(),
                                        exam.subtitle(),
                                        exam.durationMinutes(),
                                        exam.sections(),
                                        resumable))
                .toList();
    }

    @Override
    public AttemptHandleDTO start(String email) {
        AccountAccessService account = accounts.getIfAvailable();
        ExamAttemptService attempt = attempts.getIfAvailable();
        if (account == null || attempt == null)
            throw new BusinessRuleException(
                    "Luồng Placement chưa thể bắt đầu vì engine lượt thi của hệ thống chưa sẵn sàng");
        Integer userId = account.requireActiveStudent(email);
        PlacementExam exam =
                available().stream()
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new BusinessRuleException(
                                                "Chưa có đề Placement tổng hợp đã công bố"));
        examCatalog.publishedExam(userId, exam.id());
        return attempt.startPlacement(
                userId,
                exam.id(),
                "placement:" + userId + ":" + exam.id() + ":" + UUID.randomUUID());
    }

    @Override
    public List<PlacementOption> available(String email, String skill) {
        List<PlacementExam> exams = available(skill);
        AccountAccessService account = accounts.getIfAvailable();
        ExamAttemptService attempt = attempts.getIfAvailable();
        boolean resumable =
                account != null
                        && attempt != null
                        && attempt.hasInProgressPlacement(
                                account.requireActiveStudent(email), skill);
        return exams.stream()
                .map(
                        exam ->
                                new PlacementOption(
                                        exam.id(),
                                        exam.title(),
                                        exam.subtitle(),
                                        exam.durationMinutes(),
                                        exam.sections(),
                                        resumable))
                .toList();
    }

    @Override
    public AttemptHandleDTO start(String email, String skill) {
        return start(email, skill, null);
    }

    @Override
    public AttemptHandleDTO start(String email, String skill, Integer examId) {
        AccountAccessService account = accounts.getIfAvailable();
        ExamAttemptService attempt = attempts.getIfAvailable();
        if (account == null || attempt == null)
            throw new BusinessRuleException(
                    "Luồng Placement chưa thể bắt đầu vì engine lượt thi của hệ thống chưa sẵn sàng");
        Integer userId = account.requireActiveStudent(email);
        PlacementExam exam =
                available(skill).stream()
                        .filter(value -> examId == null || Objects.equals(value.id(), examId))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new BusinessRuleException(
                                                "Chưa có đề Placement đã công bố cho kỹ năng này"));
        // Legacy callers may still select a skill. Keep the old overlap contract for
        // those requests while the current UI uses start(email) for the single
        // combined assessment. A different placement cannot be opened while one is
        // still active, but retrying the same skill resumes its existing attempt.
        if (attempt.hasInProgressPlacement(userId)
                && !attempt.hasInProgressPlacement(userId, skill)) {
            throw new BusinessRuleException(
                    "Bạn còn một bài kiểm tra đầu vào đang làm. Hãy hoàn tất bài đó trước khi bắt đầu bài mới.");
        }
        examCatalog.publishedExam(userId, exam.id());
        // A new Placement/Practice run must not reuse the key of a terminal attempt.
        // Repeated clicks in the same request are still protected by the engine's
        // active-attempt lookup and the user-row lock.
        return attempt.startPlacement(
                userId,
                exam.id(),
                "placement:" + userId + ":" + exam.id() + ":" + UUID.randomUUID());
    }

    @Override
    public AttemptResultDTO result(String email, Integer attemptId) {
        AccountAccessService account = accounts.getIfAvailable();
        ExamAttemptService attempt = attempts.getIfAvailable();
        if (account == null || attempt == null)
            throw new BusinessRuleException("Luồng kết quả Placement chưa sẵn sàng");
        return attempt.result(account.requireActiveStudent(email), attemptId);
    }
}

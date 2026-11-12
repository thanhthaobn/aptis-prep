package com.aptisprep.mvc.service.placement.impl;

import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.service.account.AccountAccessService;
import com.aptisprep.mvc.service.exam.ExamAttemptCommandService;
import com.aptisprep.mvc.service.catalog.PublishedExamCatalogService;
import com.aptisprep.mvc.service.exam.ExamAttemptQueryService;
import com.aptisprep.mvc.support.contract.IntegrationModels.AttemptHandle;
import com.aptisprep.mvc.support.contract.IntegrationModels.AttemptResult;
import com.aptisprep.mvc.service.placement.PlacementExamCatalogService;
import com.aptisprep.mvc.service.placement.PlacementService;
import com.aptisprep.mvc.service.placement.PlacementService.PlacementOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class PlacementServiceImpl implements PlacementService {
    private PlacementExamCatalogService catalog;
    private PublishedExamCatalogService examCatalog;
    private ExamAttemptCommandService attempts;
    private ExamAttemptQueryService attemptQueries;
    private AccountAccessService accounts;

    public PlacementServiceImpl(
            PlacementExamCatalogService catalog,
            PublishedExamCatalogService examCatalog,
            ExamAttemptCommandService attempts,
            ExamAttemptQueryService attemptQueries,
            AccountAccessService accounts) {
        this.catalog = catalog;
        this.examCatalog = examCatalog;
        this.attempts = attempts;
        this.attemptQueries = attemptQueries;
        this.accounts = accounts;
    }

    public List<PlacementExamCatalogService.PlacementExam> available(String skill) {
        return catalog.findPublished(skill);
    }

    @Override
    public List<PlacementExamCatalogService.PlacementExam> available() {
        return catalog.findPublished();
    }

    @Override
    public List<PlacementOption> availableForStudent(String email) {
        Integer userId = accounts.requireActiveStudent(email);
        boolean resumable = attemptQueries.hasInProgressPlacement(userId);
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
    public AttemptHandle start(String email) {
        Integer userId = accounts.requireActiveStudent(email);
        PlacementExamCatalogService.PlacementExam exam =
                available().stream()
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new BusinessRuleException(
                                                "Chưa có đề Placement tổng hợp đã công bố"));
        examCatalog.publishedExam(userId, exam.id());
        return attempts.startPlacement(
                userId,
                exam.id(),
                "placement:" + userId + ":" + exam.id() + ":" + UUID.randomUUID());
    }

    public List<PlacementOption> available(String email, String skill) {
        List<PlacementExamCatalogService.PlacementExam> exams = available(skill);
        boolean resumable =
                attemptQueries.hasInProgressPlacement(accounts.requireActiveStudent(email), skill);
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

    public AttemptHandle start(String email, String skill) {
        return start(email, skill, null);
    }

    public AttemptHandle start(String email, String skill, Integer examId) {
        Integer userId = accounts.requireActiveStudent(email);
        PlacementExamCatalogService.PlacementExam exam =
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
        if (attemptQueries.hasInProgressPlacement(userId)
                && !attemptQueries.hasInProgressPlacement(userId, skill)) {
            throw new BusinessRuleException(
                    "Bạn còn một bài kiểm tra đầu vào đang làm. Hãy hoàn tất bài đó trước khi bắt đầu bài mới.");
        }
        examCatalog.publishedExam(userId, exam.id());
        // A new Placement/Practice run must not reuse the key of a terminal attempt.
        // Repeated clicks in the same request are still protected by the engine's
        // active-attempt lookup and the user-row lock.
        return attempts.startPlacement(
                userId,
                exam.id(),
                "placement:" + userId + ":" + exam.id() + ":" + UUID.randomUUID());
    }

    public AttemptResult result(String email, Integer attemptId) {
        return attemptQueries.result(accounts.requireActiveStudent(email), attemptId);
    }
}








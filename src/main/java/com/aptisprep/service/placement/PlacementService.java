package com.aptisprep.service.placement;

import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.service.contract.AccountAccessContract;
import com.aptisprep.service.contract.ExamAttemptContract;
import com.aptisprep.service.contract.ExamCatalogContract;
import com.aptisprep.service.contract.IntegrationModels.AttemptHandle;
import com.aptisprep.service.contract.IntegrationModels.AttemptResult;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class PlacementService {
    private final PlacementExamCatalogService catalog;
    private final ExamCatalogContract examCatalog;
    private final ObjectProvider<ExamAttemptContract> attempts;
    private final ObjectProvider<AccountAccessContract> accounts;

    public PlacementService(
            PlacementExamCatalogService catalog,
            ExamCatalogContract examCatalog,
            ObjectProvider<ExamAttemptContract> attempts,
            ObjectProvider<AccountAccessContract> accounts) {
        this.catalog = catalog;
        this.examCatalog = examCatalog;
        this.attempts = attempts;
        this.accounts = accounts;
    }

    public List<PlacementExamCatalogService.PlacementExam> available(String skill) {
        return catalog.findPublished(skill);
    }

    public AttemptHandle start(String email, String skill) {
        return start(email, skill, null);
    }

    public AttemptHandle start(String email, String skill, Integer examId) {
        AccountAccessContract account = accounts.getIfAvailable();
        ExamAttemptContract attempt = attempts.getIfAvailable();
        if (account == null || attempt == null)
            throw new BusinessRuleException(
                    "Luồng Placement chưa thể bắt đầu vì engine lượt thi của hệ thống chưa sẵn sàng");
        Integer userId = account.requireActiveStudent(email);
        PlacementExamCatalogService.PlacementExam exam =
                available(skill).stream()
                        .filter(value -> examId == null || Objects.equals(value.id(), examId))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new BusinessRuleException(
                                                "Chưa có đề Placement đã công bố cho kỹ năng này"));
        examCatalog.publishedExam(userId, exam.id());
        // A new Placement/Practice run must not reuse the key of a terminal attempt.
        // Repeated clicks in the same request are still protected by the engine's
        // active-attempt lookup and the user-row lock.
        return attempt.startPlacement(
                userId,
                exam.id(),
                "placement:" + userId + ":" + exam.id() + ":" + UUID.randomUUID());
    }

    public AttemptResult result(String email, Integer attemptId) {
        AccountAccessContract account = accounts.getIfAvailable();
        ExamAttemptContract attempt = attempts.getIfAvailable();
        if (account == null || attempt == null)
            throw new BusinessRuleException("Luồng kết quả Placement chưa sẵn sàng");
        return attempt.result(account.requireActiveStudent(email), attemptId);
    }
}

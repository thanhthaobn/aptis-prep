package com.aptisprep.service.placement;

import com.aptisprep.dto.view.AdminCatalogPreview.Entry;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.service.admin.CatalogService;
import com.aptisprep.service.contract.AccountAccessContract;
import com.aptisprep.service.contract.ExamAttemptContract;
import com.aptisprep.service.contract.ExamCatalogContract;
import com.aptisprep.service.contract.IntegrationModels.AttemptHandle;
import com.aptisprep.service.contract.IntegrationModels.AttemptResult;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class PlacementService {
    private final CatalogService catalog;
    private final ExamCatalogContract examCatalog;
    private final ObjectProvider<ExamAttemptContract> attempts;
    private final ObjectProvider<AccountAccessContract> accounts;

    public PlacementService(CatalogService catalog, ExamCatalogContract examCatalog, ObjectProvider<ExamAttemptContract> attempts, ObjectProvider<AccountAccessContract> accounts) {
        this.catalog = catalog; this.examCatalog = examCatalog; this.attempts = attempts; this.accounts = accounts;
    }

    public List<Entry> available(String skill) { return catalog.find("exams", "", skill, "PUBLISHED", "PLACEMENT"); }

    public AttemptHandle start(String email, String skill) {
        AccountAccessContract account = accounts.getIfAvailable();
        ExamAttemptContract attempt = attempts.getIfAvailable();
        if (account == null || attempt == null) throw new BusinessRuleException("Luồng Placement chưa thể bắt đầu vì engine lượt thi của hệ thống chưa sẵn sàng");
        Integer userId = account.requireActiveStudent(email);
        Entry exam = available(skill).stream().findFirst().orElseThrow(() -> new BusinessRuleException("Chưa có đề Placement đã công bố cho kỹ năng này"));
        examCatalog.publishedExam(userId, exam.id());
        return attempt.start(userId, exam.id(), "placement-" + UUID.randomUUID());
    }

    public AttemptResult result(String email, Integer attemptId) {
        AccountAccessContract account = accounts.getIfAvailable();
        ExamAttemptContract attempt = attempts.getIfAvailable();
        if (account == null || attempt == null) throw new BusinessRuleException("Luồng kết quả Placement chưa sẵn sàng");
        return attempt.result(account.requireActiveStudent(email), attemptId);
    }
}

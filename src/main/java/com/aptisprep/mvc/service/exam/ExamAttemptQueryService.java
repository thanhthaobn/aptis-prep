package com.aptisprep.mvc.service.exam;

import com.aptisprep.mvc.support.contract.IntegrationModels.AdminHistoryFilter;
import com.aptisprep.mvc.support.contract.IntegrationModels.AdminHistoryPage;
import com.aptisprep.mvc.support.contract.IntegrationModels.AttemptResult;
import com.aptisprep.mvc.support.contract.IntegrationModels.HistoryFilter;
import com.aptisprep.mvc.support.contract.IntegrationModels.HistoryPage;
import java.util.Optional;

/** Read-only attempt queries shared by account, learning, placement, and admin flows. */
public interface ExamAttemptQueryService {
    Integer latestResumableAttemptId(Integer userId, Integer examId);

    AttemptResult result(Integer userId, Integer attemptId);

    HistoryPage history(Integer userId, int page, int size, HistoryFilter filter);

    AdminHistoryPage adminHistory(String adminEmail, int page, int size, AdminHistoryFilter filter);

    AttemptResult adminResult(String adminEmail, Integer attemptId);

    boolean hasInProgressPlacement(Integer userId, String skill);

    boolean hasInProgressPlacement(Integer userId);

    boolean hasPlacementAttempt(Integer userId);

    Optional<Integer> checkpointOriginPathId(Integer userId, Integer attemptId);
}

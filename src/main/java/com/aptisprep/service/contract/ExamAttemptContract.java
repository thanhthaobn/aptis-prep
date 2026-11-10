package com.aptisprep.service.contract;

import com.aptisprep.service.contract.IntegrationModels.*;
import java.util.Optional;

/** Lifecycle owner: Trâm. All methods recheck ownership/account status; grading is delegated to Thông. */
public interface ExamAttemptContract {
    default boolean hasInProgressPlacement(Integer userId, String skill) {
        return false;
    }

    default boolean hasInProgressPlacement(Integer userId) {
        return false;
    }

    default boolean hasPlacementAttempt(Integer userId) {
        return false;
    }

    AttemptHandle start(Integer userId, Integer examId, String requestKey);

    Integer latestResumableAttemptId(Integer userId, Integer examId);

    AttemptHandle startPlacement(Integer userId, Integer examId, String requestKey);

    AttemptHandle startCheckpoint(
            Integer userId, Integer examId, Integer pathId, String requestKey);

    Optional<Integer> checkpointOriginPathId(Integer userId, Integer attemptId);

    void submit(Integer userId, Integer attemptId, String requestKey);

    AttemptResult result(Integer userId, Integer attemptId);

    default HistoryPage history(Integer userId, int page, int size) {
        return history(userId, page, size, HistoryFilter.empty());
    }

    HistoryPage history(Integer userId, int page, int size, HistoryFilter filter);

    AdminHistoryPage adminHistory(String adminEmail, int page, int size, AdminHistoryFilter filter);

    AttemptResult adminResult(String adminEmail, Integer attemptId);

    void retryFailedGrading(Integer userId, Integer attemptId, String requestKey);

    boolean canRegradeEmpty(Integer userId, Integer attemptId);

    void regradeEmpty(Integer userId, Integer attemptId);
}

package com.aptisprep.service.contract;

import com.aptisprep.service.contract.IntegrationModels.*;

/** Owner 4. All methods recheck ownership/account status; no network call in DB transaction. */
public interface ExamAttemptContract {
    AttemptHandle start(Integer userId, Integer examId, String requestKey);
    void submit(Integer userId, Integer attemptId, String requestKey);
    AttemptResult result(Integer userId, Integer attemptId);
    HistoryPage history(Integer userId, int page, int size);
    void retryFailedGrading(Integer userId, Integer attemptId, String requestKey);
}

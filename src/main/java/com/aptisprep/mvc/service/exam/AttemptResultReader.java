package com.aptisprep.mvc.service.exam;

import com.aptisprep.mvc.entity.ExamAttempt;
import com.aptisprep.mvc.entity.MockExam;
import com.aptisprep.mvc.support.contract.*;
import com.aptisprep.mvc.support.contract.IntegrationModels.AdminHistoryFilter;
import com.aptisprep.mvc.support.contract.IntegrationModels.AdminHistoryPage;
import com.aptisprep.mvc.support.contract.IntegrationModels.AttemptResult;
import com.aptisprep.mvc.support.contract.IntegrationModels.HistoryFilter;
import com.aptisprep.mvc.support.contract.IntegrationModels.HistoryPage;

/** Attempt result read service contract. */
public interface AttemptResultReader {
    public HistoryPage history(Integer userId, int page, int size, HistoryFilter filter);

    public AdminHistoryPage adminHistory(
            String adminEmail, int page, int size, AdminHistoryFilter filter);

    public AttemptResult adminResult(String adminEmail, Integer attemptId);

    public AttemptResult view(ExamAttempt attempt, MockExam exam);
}

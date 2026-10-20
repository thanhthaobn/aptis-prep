package com.aptisprep.service.placement;

import com.aptisprep.event.AttemptCompletedEvent;
import com.aptisprep.service.account.AccountAccessService;
import com.aptisprep.service.contract.LearningPathContract;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PlacementOrchestrationService {
    private final AccountAccessService accounts;
    private final ObjectProvider<LearningPathContract> paths;
    public PlacementOrchestrationService(AccountAccessService accounts, ObjectProvider<LearningPathContract> paths) { this.accounts = accounts; this.paths = paths; }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCompleted(AttemptCompletedEvent event) {
        if ("PLACEMENT".equals(event.examType())) {
            accounts.applyAssessment(event.userId(), event.attemptId());
            LearningPathContract path = paths.getIfAvailable(); if (path != null) path.ensurePaths(event.userId());
        } else if ("CHECKPOINT".equals(event.examType())) {
            LearningPathContract path = paths.getIfAvailable(); if (path != null) path.applyCheckpoint(event.userId(), event.attemptId());
        }
    }
}

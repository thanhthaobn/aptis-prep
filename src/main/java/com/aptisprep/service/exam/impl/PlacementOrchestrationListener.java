package com.aptisprep.service.exam.impl;

import com.aptisprep.event.AttemptCompletedEvent;
import com.aptisprep.service.account.AccountAccessService;
import com.aptisprep.service.exam.AttemptOutboxService;
import com.aptisprep.service.learning.LearningPathService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class PlacementOrchestrationListener {
    private final AccountAccessService accounts;
    private final ObjectProvider<LearningPathService> paths;
    private final AttemptOutboxService outbox;
    private final TransactionTemplate transactions;

    public PlacementOrchestrationListener(
            AccountAccessService accounts,
            ObjectProvider<LearningPathService> paths,
            AttemptOutboxService outbox,
            PlatformTransactionManager transactionManager) {
        this.accounts = accounts;
        this.paths = paths;
        this.outbox = outbox;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onCompleted(AttemptCompletedEvent event) {
        transactions.executeWithoutResult(
                status -> {
                    if ("PLACEMENT".equals(event.examType())) {
                        accounts.applyAssessment(event.userId(), event.attemptId());
                        requirePath().ensurePaths(event.userId());
                    } else if ("CHECKPOINT".equals(event.examType())) {
                        requirePath().applyCheckpoint(event.userId(), event.attemptId());
                    }
                });
        // A failed transaction throws before this line, leaving PROCESSING in
        // the outbox so recovery can replay the event.
        outbox.markDelivered(event);
    }

    private LearningPathService requirePath() {
        LearningPathService service = paths.getIfAvailable();
        if (service == null)
            throw new IllegalStateException("LearningPathService is not available");
        return service;
    }
}

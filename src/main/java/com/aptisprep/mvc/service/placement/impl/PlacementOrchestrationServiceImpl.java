package com.aptisprep.mvc.service.placement.impl;

import com.aptisprep.mvc.event.AttemptCompletedEvent;
import com.aptisprep.mvc.service.placement.PlacementOrchestrationService;
import com.aptisprep.mvc.service.account.AccountAccessService;
import com.aptisprep.mvc.service.learning.LearningPathService;
import com.aptisprep.mvc.service.exam.AttemptOutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PlacementOrchestrationServiceImpl implements PlacementOrchestrationService {
    private AccountAccessService accounts;
    private LearningPathService paths;
    private AttemptOutboxService outbox;
    private TransactionTemplate transactions;

    public PlacementOrchestrationServiceImpl(
            AccountAccessService accounts,
            LearningPathService paths,
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
        return paths;
    }
}










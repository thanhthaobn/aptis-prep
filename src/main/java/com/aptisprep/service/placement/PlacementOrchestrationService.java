package com.aptisprep.service.placement;

import com.aptisprep.event.AttemptCompletedEvent;
import com.aptisprep.service.exam.AttemptOutboxService;
import com.aptisprep.service.account.AccountAccessService;
import com.aptisprep.service.contract.LearningPathContract;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PlacementOrchestrationService {
    private final AccountAccessService accounts;
    private final ObjectProvider<LearningPathContract> paths;
    private final AttemptOutboxService outbox;
    private final TransactionTemplate transactions;
    public PlacementOrchestrationService(AccountAccessService accounts, ObjectProvider<LearningPathContract> paths,
            AttemptOutboxService outbox, PlatformTransactionManager transactionManager) {
        this.accounts = accounts; this.paths = paths; this.outbox = outbox;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCompleted(AttemptCompletedEvent event) {
        transactions.executeWithoutResult(status -> {
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

    private LearningPathContract requirePath() {
        LearningPathContract path = paths.getIfAvailable();
        if (path == null) throw new IllegalStateException("Learning Path chưa sẵn sàng");
        return path;
    }
}

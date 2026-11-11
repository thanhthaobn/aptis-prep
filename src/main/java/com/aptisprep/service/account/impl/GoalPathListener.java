package com.aptisprep.service.account.impl;

import com.aptisprep.event.GoalChangedEvent;
import com.aptisprep.service.learning.LearningPathService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class GoalPathListener {
    private final ObjectProvider<LearningPathService> paths;
    private final TransactionTemplate transactions;

    public GoalPathListener(
            ObjectProvider<LearningPathService> paths,
            PlatformTransactionManager transactionManager) {
        this.paths = paths;
        this.transactions = new TransactionTemplate(transactionManager);
        this.transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGoalChanged(GoalChangedEvent event) {
        LearningPathService contract = paths.getIfAvailable();
        if (contract != null)
            transactions.executeWithoutResult(status -> contract.ensurePaths(event.userId()));
    }
}

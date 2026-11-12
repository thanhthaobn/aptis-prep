package com.aptisprep.mvc.support.account;

import com.aptisprep.mvc.event.GoalChangedEvent;
import com.aptisprep.mvc.service.learning.LearningPathService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class GoalPathListener {
    private final LearningPathService paths;
    private final TransactionTemplate transactions;

    public GoalPathListener(
            LearningPathService paths, PlatformTransactionManager transactionManager) {
        this.paths = paths;
        this.transactions = new TransactionTemplate(transactionManager);
        this.transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGoalChanged(GoalChangedEvent event) {
        transactions.executeWithoutResult(status -> paths.ensurePaths(event.userId()));
    }
}

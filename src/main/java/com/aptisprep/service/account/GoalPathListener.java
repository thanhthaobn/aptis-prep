package com.aptisprep.service.account;

import com.aptisprep.event.GoalChangedEvent;
import com.aptisprep.service.contract.LearningPathContract;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class GoalPathListener {
    private final ObjectProvider<LearningPathContract> paths;
    public GoalPathListener(ObjectProvider<LearningPathContract> paths) { this.paths = paths; }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGoalChanged(GoalChangedEvent event) {
        LearningPathContract contract = paths.getIfAvailable();
        if (contract != null) contract.ensurePaths(event.userId());
    }
}

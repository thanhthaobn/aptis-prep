package com.aptisprep.service.exam;

import com.aptisprep.config.AiProperties;
import com.aptisprep.event.AiAssessmentCompletedEvent;
import com.aptisprep.event.AttemptSubmittedEvent;
import com.aptisprep.service.contract.AiAssessmentContract;
import com.aptisprep.service.contract.IntegrationModels.LockedAnswer;
import com.aptisprep.service.contract.IntegrationModels.LockedAttempt;
import com.aptisprep.service.grading.AttemptFinalizationService;
import java.math.BigDecimal;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AttemptGradingOrchestrationService {
    private final AiProperties properties;
    private final AiAssessmentContract ai;
    private final AttemptFinalizationService finalization;
    private final AttemptOutboxService outbox;

    public AttemptGradingOrchestrationService(AiProperties properties, AiAssessmentContract ai,
            AttemptFinalizationService finalization, AttemptOutboxService outbox) {
        this.properties = properties; this.ai = ai; this.finalization = finalization; this.outbox = outbox;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmitted(AttemptSubmittedEvent event) {
        if (!properties.isEnabled()) {
            finalization.markSubjectiveUnavailable(event, "AI_DISABLED");
            outbox.markDelivered(event);
            return;
        }
        if (!finalization.accepts(event)) {
            outbox.markDelivered(event);
            return;
        }
        LockedAttempt locked = event.lockedAttempt();
        if (locked == null) {
            outbox.markDelivered(event);
            return;
        }
        for (LockedAnswer answer : locked.answers()) {
            if (!"WRITING_TEXT".equals(answer.questionType()) && !"SPEAKING_AUDIO".equals(answer.questionType())) continue;
            String submission = answer.lockedContent();
            if (submission == null || submission.isBlank()) continue;
            String taskType = "WRITING_TEXT".equals(answer.questionType()) ? "WRITING" : "SPEAKING";
            String key = "attempt:" + event.attemptId() + ":answer:" + answer.answerId() + ":v" + locked.answerVersion();
            AiAssessmentContract.AssessmentRequest request = new AiAssessmentContract.AssessmentRequest(key,
                    event.userId(), event.attemptId(), answer.answerId(), locked.answerVersion(), answer.skill(), taskType,
                    answer.taskContent(), submission, locked.rubricVersion(), locked.policyVersion(), BigDecimal.valueOf(50),
                    answer.transcript());
            ai.request(request);
            ai.process(key);
        }
        finalization.markSubjectiveUnavailable(event, null);
        outbox.markDelivered(event);
    }

    @EventListener
    public void onAssessmentCompleted(AiAssessmentCompletedEvent event) { finalization.applyAiAssessment(event); }
}

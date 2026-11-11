package com.aptisprep.service.exam.impl;

import com.aptisprep.config.AiProperties;
import com.aptisprep.event.AiAssessmentCompletedEvent;
import com.aptisprep.event.AttemptSubmittedEvent;
import com.aptisprep.presentation.AiAssessmentDTO;
import com.aptisprep.presentation.LockedAnswerDTO;
import com.aptisprep.presentation.LockedAttemptDTO;
import com.aptisprep.service.exam.AttemptOutboxService;
import com.aptisprep.service.grading.AiAssessmentService;
import com.aptisprep.service.grading.AttemptFinalizationService;
import com.aptisprep.service.grading.impl.GradingContextFormatter;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AttemptGradingOrchestrationListener {
    private final AiProperties properties;
    private final AiAssessmentService ai;
    private final AttemptFinalizationService finalization;
    private final AttemptOutboxService outbox;

    public AttemptGradingOrchestrationListener(
            AiProperties properties,
            AiAssessmentService ai,
            AttemptFinalizationService finalization,
            AttemptOutboxService outbox) {
        this.properties = properties;
        this.ai = ai;
        this.finalization = finalization;
        this.outbox = outbox;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
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
        LockedAttemptDTO locked = event.lockedAttempt();
        if (locked == null) {
            outbox.markDelivered(event);
            return;
        }
        for (LockedAnswerDTO answer : locked.answers()) {
            if (!"WRITING_TEXT".equals(answer.questionType())
                    && !"SPEAKING_AUDIO".equals(answer.questionType())) continue;
            if (!GradingContextFormatter.hasSubmission(answer)) continue;
            String submission = GradingContextFormatter.submission(answer);
            String taskType = "WRITING_TEXT".equals(answer.questionType()) ? "WRITING" : "SPEAKING";
            String key =
                    "attempt:"
                            + event.attemptId()
                            + ":answer:"
                            + answer.answerId()
                            + ":v"
                            + locked.answerVersion();
            AiAssessmentDTO.AssessmentRequest request =
                    new AiAssessmentDTO.AssessmentRequest(
                            key,
                            event.userId(),
                            event.attemptId(),
                            answer.answerId(),
                            locked.answerVersion(),
                            answer.skill(),
                            taskType,
                            GradingContextFormatter.task(answer),
                            submission,
                            locked.rubricVersion(),
                            locked.policyVersion(),
                            BigDecimal.valueOf(50),
                            answer.transcript());
            ai.request(request);
            ai.process(key);
        }
        finalization.markSubjectiveUnavailable(event, null);
        outbox.markDelivered(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onAssessmentCompleted(AiAssessmentCompletedEvent event) {
        finalization.applyAiAssessment(event);
        outbox.markDelivered(event);
    }
}

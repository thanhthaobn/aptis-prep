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

    public AttemptGradingOrchestrationService(AiProperties properties, AiAssessmentContract ai,
            AttemptFinalizationService finalization) {
        this.properties = properties; this.ai = ai; this.finalization = finalization;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmitted(AttemptSubmittedEvent event) {
        if (!properties.isEnabled()) return;
        if (!finalization.accepts(event)) return;
        LockedAttempt locked = event.lockedAttempt();
        if (locked == null) return;
        for (LockedAnswer answer : locked.answers()) {
            if (!"WRITING_TEXT".equals(answer.questionType()) && !"SPEAKING_AUDIO".equals(answer.questionType())) continue;
            String submission = answer.lockedContent();
            if (submission == null || submission.isBlank()) continue;
            String taskType = "WRITING_TEXT".equals(question.getQuestionType()) ? "WRITING" : "SPEAKING";
            String key = "attempt:" + event.attemptId() + ":answer:" + answer.getId() + ":v" + answer.getAnswerVersion();
            AiAssessmentContract.AssessmentRequest request = new AiAssessmentContract.AssessmentRequest(key, event.userId(), event.attemptId(), answer.getId(), answer.getAnswerVersion(),
                    question.getSkill(), taskType, question.getContent(), question.getInstruction() == null ? question.getContent() : question.getInstruction() + "\n" + question.getContent(),
                    "aptis-rubric-v1", "scale-v1", BigDecimal.valueOf(50), answer.getTranscript());
            ai.request(request);
            ai.process(key);
        }
    }

    @EventListener
    public void onAssessmentCompleted(AiAssessmentCompletedEvent event) { finalization.applyAiAssessment(event); }
}

package com.aptisprep.service.exam;

import com.aptisprep.config.AiProperties;
import com.aptisprep.event.AiAssessmentCompletedEvent;
import com.aptisprep.event.AttemptSubmittedEvent;
import com.aptisprep.service.contract.AiAssessmentContract;
import com.aptisprep.service.contract.IntegrationModels.LockedAnswer;
import com.aptisprep.service.contract.IntegrationModels.LockedAttempt;
import com.aptisprep.service.grading.AttemptFinalizationService;
import com.aptisprep.service.grading.SpeakingGradingService;
import com.aptisprep.service.storage.StorageService;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AttemptGradingOrchestrationService {
    private final AiProperties properties;
    private final AiAssessmentContract ai;
    private final AttemptFinalizationService finalization;
    private final SpeakingGradingService speaking;
    private final StorageService storage;
    private final AttemptOutboxService outbox;

    public AttemptGradingOrchestrationService(AiProperties properties, AiAssessmentContract ai,
            AttemptFinalizationService finalization, SpeakingGradingService speaking, StorageService storage, AttemptOutboxService outbox) {
        this.properties = properties; this.ai = ai; this.finalization = finalization;
        this.speaking = speaking; this.storage = storage;
        this.outbox = outbox;
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
            String transcript = answer.transcript();
            if ("SPEAKING".equals(taskType) && (transcript == null || transcript.isBlank())) {
                transcript = transcribeSubmission(submission);
            }
            String key = "attempt:" + event.attemptId() + ":answer:" + answer.answerId() + ":v" + locked.answerVersion();
            AiAssessmentContract.AssessmentRequest request = new AiAssessmentContract.AssessmentRequest(key,
                    event.userId(), event.attemptId(), answer.answerId(), locked.answerVersion(), answer.skill(), taskType,
                    answer.taskContent(), submission, locked.rubricVersion(), locked.policyVersion(), BigDecimal.valueOf(50),
                    transcript);
            ai.request(request);
            ai.process(key);
        }
        finalization.markSubjectiveUnavailable(event, null);
        outbox.markDelivered(event);
    }

    private String transcribeSubmission(String submission) {
        return Arrays.stream(submission.split("\\n--- ITEM ---\\n"))
                .map(String::trim).filter(value -> !value.isBlank())
                .map(path -> {
                    try {
                        return speaking.transcribe(storage.localPath(path));
                    } catch (RuntimeException exception) {
                        return null;
                    }
                })
                .filter(Objects::nonNull).filter(value -> !value.isBlank())
                .collect(Collectors.joining("\n"));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAssessmentCompleted(AiAssessmentCompletedEvent event) {
        finalization.applyAiAssessment(event);
        outbox.markDelivered(event);
    }
}

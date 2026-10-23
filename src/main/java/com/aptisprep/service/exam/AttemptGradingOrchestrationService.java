package com.aptisprep.service.exam;

import com.aptisprep.config.AiProperties;
import com.aptisprep.event.AiAssessmentCompletedEvent;
import com.aptisprep.event.AttemptSubmittedEvent;
import com.aptisprep.service.contract.AiAssessmentContract;
import com.aptisprep.service.contract.GradingContextFormatter;
import com.aptisprep.service.contract.IntegrationModels.LockedAnswer;
import com.aptisprep.service.contract.IntegrationModels.LockedAttempt;
import com.aptisprep.service.grading.AttemptFinalizationService;
import com.aptisprep.service.grading.SpeakingGradingService;
import com.aptisprep.service.storage.StorageService;
import java.math.BigDecimal;
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

    public AttemptGradingOrchestrationService(AiProperties properties, AiAssessmentContract ai,
            AttemptFinalizationService finalization, SpeakingGradingService speaking, StorageService storage) {
        this.properties = properties; this.ai = ai; this.finalization = finalization;
        this.speaking = speaking; this.storage = storage;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmitted(AttemptSubmittedEvent event) {
        if (!properties.isEnabled()) return;
        if (!finalization.accepts(event)) return;
        LockedAttempt locked = event.lockedAttempt();
        if (locked == null) return;
        for (LockedAnswer answer : locked.answers()) {
            if (!"WRITING_TEXT".equals(answer.questionType()) && !"SPEAKING_AUDIO".equals(answer.questionType())) continue;
            if (!GradingContextFormatter.hasSubmission(answer)) continue;
            String submission = GradingContextFormatter.submission(answer);
            String taskType = "WRITING_TEXT".equals(answer.questionType()) ? "WRITING" : "SPEAKING";
            String transcript = answer.transcript();
            if ("SPEAKING".equals(taskType) && (transcript == null || transcript.isBlank())) {
                transcript = transcribeSubmission(answer);
            }
            String key = "attempt:" + event.attemptId() + ":answer:" + answer.answerId() + ":v" + locked.answerVersion();
            AiAssessmentContract.AssessmentRequest request = new AiAssessmentContract.AssessmentRequest(key,
                    event.userId(), event.attemptId(), answer.answerId(), locked.answerVersion(), answer.skill(), taskType,
                    GradingContextFormatter.task(answer), submission, locked.rubricVersion(), locked.policyVersion(), BigDecimal.valueOf(50),
                    transcript);
            ai.request(request);
            ai.process(key);
        }
    }

    private String transcribeSubmission(LockedAnswer answer) {
        if (answer.items() == null || answer.items().isEmpty()) return transcribePath(answer.lockedContent());
        return answer.items().stream()
                .map(item -> {
                    String transcript = transcribePath(item.lockedContent());
                    return transcript == null || transcript.isBlank() ? null : "Mục " + item.position() + ":\n" + transcript;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n\n"));
    }

    private String transcribePath(String path) {
        if (path == null || path.isBlank()) return null;
        try {
            return speaking.transcribe(storage.localPath(path));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    @EventListener
    public void onAssessmentCompleted(AiAssessmentCompletedEvent event) { finalization.applyAiAssessment(event); }
}

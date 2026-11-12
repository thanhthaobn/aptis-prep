package com.aptisprep.mvc.service.exam.impl;

import com.aptisprep.mvc.config.AiProperties;
import com.aptisprep.mvc.dto.AiAssessmentJobDTO;
import com.aptisprep.mvc.event.AiAssessmentCompletedEvent;
import com.aptisprep.mvc.event.AttemptSubmittedEvent;
import com.aptisprep.mvc.service.exam.AttemptGradingOrchestrationService;
import com.aptisprep.mvc.service.exam.AttemptOutboxService;
import com.aptisprep.mvc.service.grading.AiAssessmentService;
import com.aptisprep.mvc.service.grading.AttemptFinalizationService;
import com.aptisprep.mvc.support.contract.*;
import com.aptisprep.mvc.support.contract.GradingContextFormatter;
import com.aptisprep.mvc.support.contract.IntegrationModels.LockedAnswer;
import com.aptisprep.mvc.support.contract.IntegrationModels.LockedAttempt;
import com.aptisprep.mvc.support.contract.IntegrationModels.LockedItem;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Kết nối luồng giao dịch của bài thi với việc chấm AI bất đồng bộ.
 *
 * <p>Event submit được xử lý sau khi transaction của lượt thi commit. Nhờ đó callback AI không
 * đọc phải đáp án chưa ghi xong và outbox có thể retry việc giao event.
 */
@Service
public class AttemptGradingOrchestrationServiceImpl implements AttemptGradingOrchestrationService {
    private AiProperties properties;
    private AiAssessmentService ai;
    private AttemptFinalizationService finalization;
    private AttemptOutboxService outbox;

    public AttemptGradingOrchestrationServiceImpl(
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
    // Tạo một AI job cho mỗi đáp án Writing/Speaking; các kỹ năng objective không gọi Gemini.
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
            AiAssessmentJobDTO request =
                    new AiAssessmentJobDTO(
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
                            answer.items().stream()
                                    .map(LockedItem::points)
                                    .map(value -> value == null ? BigDecimal.ONE : value)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add),
                            answer.transcript());
            ai.request(request);
            ai.process(key);
        }
        finalization.markSubjectiveUnavailable(event, null);
        outbox.markDelivered(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    // Chỉ nhận callback sau khi AttemptFinalizationService kiểm tra đúng lượt thi và version.
    public void onAssessmentCompleted(AiAssessmentCompletedEvent event) {
        finalization.applyAiAssessment(event);
        outbox.markDelivered(event);
    }
}

package com.aptisprep.service.exam;

import com.aptisprep.entity.AttemptOutboxEntity;
import com.aptisprep.event.AiAssessmentCompletedEvent;
import com.aptisprep.event.AttemptCompletedEvent;
import com.aptisprep.event.AttemptSubmittedEvent;
import com.aptisprep.repository.AttemptOutboxRepository;
import com.aptisprep.service.contract.IntegrationModels.LockedAttempt;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Durable replay queue for attempt events. Handlers are idempotent by attempt/event keys. */
@Service
public class AttemptOutboxService {
    private final AttemptOutboxRepository outbox;
    private final ApplicationEventPublisher events;
    private final JsonMapper json;

    public AttemptOutboxService(
            AttemptOutboxRepository outbox, ApplicationEventPublisher events, JsonMapper json) {
        this.outbox = outbox;
        this.events = events;
        this.json = json;
    }

    @Transactional
    public void enqueueSubmitted(AttemptSubmittedEvent event) {
        enqueue(
                "attempt:" + event.attemptId() + ":submitted",
                "ATTEMPT_SUBMITTED",
                event.attemptId(),
                json.writeValueAsString(
                        new SubmittedPayload(
                                event.userId(),
                                event.attemptId(),
                                event.examId(),
                                event.examType(),
                                event.lockedAttempt())));
    }

    @Transactional
    public void enqueueCompleted(AttemptCompletedEvent event) {
        enqueue(
                "attempt:" + event.attemptId() + ":completed",
                "ATTEMPT_COMPLETED",
                event.attemptId(),
                event.userId() + "|" + event.attemptId() + "|" + event.examType());
    }

    @Transactional
    public void enqueueAiAssessmentCompleted(AiAssessmentCompletedEvent event) {
        saveAiEvent(event, true);
    }

    /**
     * Backfills callback delivery for completed jobs created before the AI outbox was introduced.
     */
    @Transactional
    public void ensureAiAssessmentCompleted(AiAssessmentCompletedEvent event) {
        saveAiEvent(event, false);
    }

    @Scheduled(fixedDelayString = "${app.attempt-outbox.recovery-ms:30000}")
    @Transactional
    public void recoverPending() {
        List<AttemptOutboxEntity> pending =
                outbox.findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(
                        List.of("PENDING", "PROCESSING"), LocalDateTime.now());
        for (AttemptOutboxEntity row : pending) {
            try {
                row.setStatus("PROCESSING");
                row.setAttempts(row.getAttempts() + 1);
                // Persist the retry lease before publication: after-commit listener failures
                // cannot be caught here and must not permanently monopolize the first page.
                row.setNextAttemptAt(LocalDateTime.now().plusSeconds(backoff(row.getAttempts())));
                outbox.save(row);
                dispatch(row);
                // The event listeners run after this transaction commits. Keep the
                // row PROCESSING until the delivery marker is committed, so a
                // crash between publication and listener execution is recoverable.
                row.setLastError(null);
                outbox.save(row);
            } catch (RuntimeException exception) {
                row.setStatus("PENDING");
                row.setLastError(clean(exception.getMessage()));
                row.setNextAttemptAt(LocalDateTime.now().plusSeconds(backoff(row.getAttempts())));
                outbox.save(row);
            }
        }
    }

    private void enqueue(String key, String type, Integer aggregateId, String payload) {
        if (outbox.findByEventKey(key).isPresent()) return;
        AttemptOutboxEntity row = new AttemptOutboxEntity();
        row.setEventKey(key);
        row.setEventType(type);
        row.setAggregateId(aggregateId);
        row.setPayload(payload);
        row.setStatus("PENDING");
        outbox.save(row);
    }

    private void saveAiEvent(AiAssessmentCompletedEvent event, boolean replaceExisting) {
        String key = aiEventKey(event);
        AttemptOutboxEntity row = outbox.findByEventKey(key).orElseGet(AttemptOutboxEntity::new);
        if (row.getEventKey() != null && !replaceExisting) return;
        row.setEventKey(key);
        row.setEventType("AI_ASSESSMENT_COMPLETED");
        row.setAggregateId(event.attemptId());
        row.setPayload(
                json.writeValueAsString(
                        new AiPayload(
                                event.assessmentKey(),
                                event.userId(),
                                event.attemptId(),
                                event.answerId(),
                                event.state(),
                                event.errorMessage(),
                                event.answerVersion())));
        row.setStatus("PENDING");
        row.setAttempts(0);
        row.setNextAttemptAt(LocalDateTime.now());
        row.setLastError(null);
        row.setProcessedAt(null);
        outbox.save(row);
    }

    private String aiEventKey(AiAssessmentCompletedEvent event) {
        return "ai:" + event.assessmentKey() + ":v" + event.answerVersion() + ":" + event.state();
    }

    private void dispatch(AttemptOutboxEntity row) {
        if ("ATTEMPT_SUBMITTED".equals(row.getEventType())) {
            SubmittedPayload payload;
            try {
                payload = json.readValue(row.getPayload(), SubmittedPayload.class);
            } catch (RuntimeException legacyPayload) {
                String[] values = row.getPayload().split("\\|", -1);
                payload =
                        new SubmittedPayload(
                                Integer.valueOf(values[0]),
                                Integer.valueOf(values[1]),
                                Integer.valueOf(values[2]),
                                values[3],
                                null);
            }
            events.publishEvent(
                    new AttemptSubmittedEvent(
                            payload.userId(),
                            payload.attemptId(),
                            payload.examId(),
                            payload.examType(),
                            payload.lockedAttempt()));
        } else if ("ATTEMPT_COMPLETED".equals(row.getEventType())) {
            String[] values = row.getPayload().split("\\|", -1);
            events.publishEvent(
                    new AttemptCompletedEvent(
                            Integer.valueOf(values[0]), Integer.valueOf(values[1]), values[2]));
        } else if ("AI_ASSESSMENT_COMPLETED".equals(row.getEventType())) {
            AiPayload payload = json.readValue(row.getPayload(), AiPayload.class);
            events.publishEvent(
                    new AiAssessmentCompletedEvent(
                            payload.assessmentKey(),
                            payload.userId(),
                            payload.attemptId(),
                            payload.answerId(),
                            payload.state(),
                            payload.errorMessage(),
                            payload.answerVersion()));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDelivered(AttemptSubmittedEvent event) {
        markDeliveredKey("attempt:" + event.attemptId() + ":submitted");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDelivered(AttemptCompletedEvent event) {
        markDeliveredKey("attempt:" + event.attemptId() + ":completed");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDelivered(AiAssessmentCompletedEvent event) {
        markDeliveredKey(aiEventKey(event));
    }

    private void markDeliveredKey(String key) {
        outbox.findByEventKey(key)
                .ifPresent(
                        row -> {
                            row.setStatus("DISPATCHED");
                            row.setProcessedAt(LocalDateTime.now());
                            row.setLastError(null);
                            outbox.save(row);
                        });
    }

    private long backoff(int attempts) {
        return Math.min(900, Math.max(30, attempts * 30L));
    }

    private String clean(String value) {
        return value == null || value.isBlank()
                ? "Không thể phát event"
                : value.substring(0, Math.min(1000, value.length()));
    }

    private record SubmittedPayload(
            Integer userId,
            Integer attemptId,
            Integer examId,
            String examType,
            LockedAttempt lockedAttempt) {}

    private record AiPayload(
            String assessmentKey,
            Integer userId,
            Integer attemptId,
            Integer answerId,
            String state,
            String errorMessage,
            Integer answerVersion) {}
}

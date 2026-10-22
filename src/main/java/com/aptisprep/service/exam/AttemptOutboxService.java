package com.aptisprep.service.exam;

import com.aptisprep.entity.AttemptOutboxEntity;
import com.aptisprep.event.AttemptCompletedEvent;
import com.aptisprep.event.AttemptSubmittedEvent;
import com.aptisprep.repository.AttemptOutboxRepository;
import com.aptisprep.service.contract.IntegrationModels.LockedAttempt;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import tools.jackson.databind.json.JsonMapper;

/** Durable replay queue for attempt events. Handlers are idempotent by attempt/event keys. */
@Service
public class AttemptOutboxService {
    private final AttemptOutboxRepository outbox;
    private final ApplicationEventPublisher events;
    private final JsonMapper json = JsonMapper.builder().build();
    public AttemptOutboxService(AttemptOutboxRepository outbox, ApplicationEventPublisher events) { this.outbox = outbox; this.events = events; }

    @Transactional
    public void enqueueSubmitted(AttemptSubmittedEvent event) {
        enqueue("attempt:" + event.attemptId() + ":submitted", "ATTEMPT_SUBMITTED", event.attemptId(),
                json.writeValueAsString(new SubmittedPayload(event.userId(), event.attemptId(), event.examId(), event.examType(), event.lockedAttempt())));
    }
    @Transactional
    public void enqueueCompleted(AttemptCompletedEvent event) { enqueue("attempt:" + event.attemptId() + ":completed", "ATTEMPT_COMPLETED", event.attemptId(), event.userId() + "|" + event.attemptId() + "|" + event.examType()); }

    @Scheduled(fixedDelayString = "${app.attempt-outbox.recovery-ms:30000}")
    @Transactional
    public void recoverPending() {
        List<AttemptOutboxEntity> pending = outbox.findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(List.of("PENDING", "PROCESSING"), LocalDateTime.now());
        for (AttemptOutboxEntity row : pending) {
            try {
                row.setStatus("PROCESSING"); row.setAttempts(row.getAttempts() + 1); outbox.save(row);
                dispatch(row);
                // The event listeners run after this transaction commits. Keep the
                // row PROCESSING until the delivery marker is committed, so a
                // crash between publication and listener execution is recoverable.
                row.setLastError(null); outbox.save(row);
            } catch (RuntimeException exception) {
                row.setStatus("PENDING"); row.setLastError(clean(exception.getMessage())); row.setNextAttemptAt(LocalDateTime.now().plusSeconds(backoff(row.getAttempts()))); outbox.save(row);
            }
        }
    }

    private void enqueue(String key, String type, Integer aggregateId, String payload) {
        if (outbox.findByEventKey(key).isPresent()) return;
        AttemptOutboxEntity row = new AttemptOutboxEntity(); row.setEventKey(key); row.setEventType(type); row.setAggregateId(aggregateId); row.setPayload(payload); row.setStatus("PENDING"); outbox.save(row);
    }
    private void dispatch(AttemptOutboxEntity row) {
        if ("ATTEMPT_SUBMITTED".equals(row.getEventType())) {
            SubmittedPayload payload;
            try {
                payload = json.readValue(row.getPayload(), SubmittedPayload.class);
            } catch (RuntimeException legacyPayload) {
                String[] values = row.getPayload().split("\\|", -1);
                payload = new SubmittedPayload(Integer.valueOf(values[0]), Integer.valueOf(values[1]),
                        Integer.valueOf(values[2]), values[3], null);
            }
            events.publishEvent(new AttemptSubmittedEvent(payload.userId(), payload.attemptId(), payload.examId(), payload.examType(), payload.lockedAttempt()));
        } else if ("ATTEMPT_COMPLETED".equals(row.getEventType())) {
            String[] values = row.getPayload().split("\\|", -1);
            events.publishEvent(new AttemptCompletedEvent(Integer.valueOf(values[0]), Integer.valueOf(values[1]), values[2]));
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

    private void markDeliveredKey(String key) {
        outbox.findByEventKey(key).ifPresent(row -> {
            row.setStatus("DISPATCHED"); row.setProcessedAt(LocalDateTime.now()); row.setLastError(null); outbox.save(row);
        });
    }
    private long backoff(int attempts) { return Math.min(900, Math.max(30, attempts * 30L)); }
    private String clean(String value) { return value == null || value.isBlank() ? "Không thể phát event" : value.substring(0, Math.min(1000, value.length())); }
    private record SubmittedPayload(Integer userId, Integer attemptId, Integer examId, String examType, LockedAttempt lockedAttempt) {}
}

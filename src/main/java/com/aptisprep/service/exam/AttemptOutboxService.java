package com.aptisprep.service.exam;

import com.aptisprep.entity.AttemptOutboxEntity;
import com.aptisprep.event.AttemptCompletedEvent;
import com.aptisprep.event.AttemptSubmittedEvent;
import com.aptisprep.repository.AttemptOutboxRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Durable replay queue for attempt events. Handlers are idempotent by attempt/event keys. */
@Service
public class AttemptOutboxService {
    private final AttemptOutboxRepository outbox;
    private final ApplicationEventPublisher events;
    public AttemptOutboxService(AttemptOutboxRepository outbox, ApplicationEventPublisher events) { this.outbox = outbox; this.events = events; }

    @Transactional
    public void enqueueSubmitted(AttemptSubmittedEvent event) { enqueue("attempt:" + event.attemptId() + ":submitted", "ATTEMPT_SUBMITTED", event.attemptId(), event.userId() + "|" + event.attemptId() + "|" + event.examId() + "|" + event.examType()); }
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
                row.setStatus("DISPATCHED"); row.setProcessedAt(LocalDateTime.now()); row.setLastError(null); outbox.save(row);
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
        String[] values = row.getPayload().split("\\|", -1);
        if ("ATTEMPT_SUBMITTED".equals(row.getEventType())) events.publishEvent(new AttemptSubmittedEvent(Integer.valueOf(values[0]), Integer.valueOf(values[1]), Integer.valueOf(values[2]), values[3]));
        else if ("ATTEMPT_COMPLETED".equals(row.getEventType())) events.publishEvent(new AttemptCompletedEvent(Integer.valueOf(values[0]), Integer.valueOf(values[1]), values[2]));
    }
    private long backoff(int attempts) { return Math.min(900, Math.max(30, attempts * 30L)); }
    private String clean(String value) { return value == null || value.isBlank() ? "Không thể phát event" : value.substring(0, Math.min(1000, value.length())); }
}

package com.aptisprep.service.exam;

import com.aptisprep.entity.AttemptOutboxEntity;
import com.aptisprep.event.AiAssessmentCompletedEvent;
import com.aptisprep.event.AttemptCompletedEvent;
import com.aptisprep.event.AttemptSubmittedEvent;
import com.aptisprep.repository.AttemptOutboxRepository;
import com.aptisprep.repository.ExamAttemptRepository;
import com.aptisprep.repository.MockExamRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Durable replay queue for attempt events. Handlers are idempotent by attempt/event keys. */
@Service
public class AttemptOutboxService {
    private final AttemptOutboxRepository attemptOutboxRepository;
    private final ApplicationEventPublisher events;
    private final ExamAttemptRepository attempts;
    private final MockExamRepository exams;
    private final ExamGradingPayloadFactory gradingPayloads;

    public AttemptOutboxService(
            AttemptOutboxRepository attemptOutboxRepository,
            ApplicationEventPublisher events,
            ExamAttemptRepository attempts,
            MockExamRepository exams,
            ExamGradingPayloadFactory gradingPayloads) {
        this.attemptOutboxRepository = attemptOutboxRepository;
        this.events = events;
        this.attempts = attempts;
        this.exams = exams;
        this.gradingPayloads = gradingPayloads;
    }

    @Transactional
    public void enqueueSubmitted(AttemptSubmittedEvent event) {
        enqueue(
                "attempt:" + event.attemptId() + ":submitted",
                "ATTEMPT_SUBMITTED",
                event.attemptId(),
                flat(event.userId(), event.attemptId(), event.examId(), event.examType()));
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
                attemptOutboxRepository
                        .findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(
                                List.of("PENDING", "PROCESSING"), LocalDateTime.now());
        for (AttemptOutboxEntity row : pending) {
            try {
                row.setStatus("PROCESSING");
                row.setAttempts(row.getAttempts() + 1);
                // Persist the retry lease before publication: after-commit listener failures
                // cannot be caught here and must not permanently monopolize the first page.
                row.setNextAttemptAt(LocalDateTime.now().plusSeconds(backoff(row.getAttempts())));
                attemptOutboxRepository.save(row);
                dispatch(row);
                // The event listeners run after this transaction commits. Keep the
                // row PROCESSING until the delivery marker is committed, so a
                // crash between publication and listener execution is recoverable.
                row.setLastError(null);
                attemptOutboxRepository.save(row);
            } catch (RuntimeException exception) {
                row.setStatus("PENDING");
                row.setLastError(clean(exception.getMessage()));
                row.setNextAttemptAt(LocalDateTime.now().plusSeconds(backoff(row.getAttempts())));
                attemptOutboxRepository.save(row);
            }
        }
    }

    private void enqueue(String key, String type, Integer aggregateId, String payload) {
        if (attemptOutboxRepository.findByEventKey(key).isPresent()) return;
        AttemptOutboxEntity row = new AttemptOutboxEntity();
        row.setEventKey(key);
        row.setEventType(type);
        row.setAggregateId(aggregateId);
        row.setPayload(payload);
        row.setStatus("PENDING");
        attemptOutboxRepository.save(row);
    }

    private void saveAiEvent(AiAssessmentCompletedEvent event, boolean replaceExisting) {
        String key = aiEventKey(event);
        AttemptOutboxEntity row =
                attemptOutboxRepository.findByEventKey(key).orElseGet(AttemptOutboxEntity::new);
        if (row.getEventKey() != null && !replaceExisting) return;
        row.setEventKey(key);
        row.setEventType("AI_ASSESSMENT_COMPLETED");
        row.setAggregateId(event.attemptId());
        row.setPayload(
                flat(
                        event.assessmentKey(),
                        event.userId(),
                        event.attemptId(),
                        event.answerId(),
                        event.state(),
                        event.errorMessage(),
                        event.answerVersion()));
        row.setStatus("PENDING");
        row.setAttempts(0);
        row.setNextAttemptAt(LocalDateTime.now());
        row.setLastError(null);
        row.setProcessedAt(null);
        attemptOutboxRepository.save(row);
    }

    private String aiEventKey(AiAssessmentCompletedEvent event) {
        return "ai:" + event.assessmentKey() + ":v" + event.answerVersion() + ":" + event.state();
    }

    private void dispatch(AttemptOutboxEntity row) {
        if ("ATTEMPT_SUBMITTED".equals(row.getEventType())) {
            String[] values = row.getPayload().split("\\|", -1);
            Integer attemptId = Integer.valueOf(decode(values[1]));
            var attempt = attempts.findById(attemptId).orElseThrow();
            var exam = exams.findById(attempt.getMockExamId()).orElseThrow();
            events.publishEvent(
                    new AttemptSubmittedEvent(
                            Integer.valueOf(decode(values[0])),
                            attemptId,
                            Integer.valueOf(decode(values[2])),
                            decode(values[3]),
                            gradingPayloads.build(attempt, exam)));
        } else if ("ATTEMPT_COMPLETED".equals(row.getEventType())) {
            String[] values = row.getPayload().split("\\|", -1);
            events.publishEvent(
                    new AttemptCompletedEvent(
                            Integer.valueOf(values[0]), Integer.valueOf(values[1]), values[2]));
        } else if ("AI_ASSESSMENT_COMPLETED".equals(row.getEventType())) {
            String[] values = row.getPayload().split("\\|", -1);
            events.publishEvent(
                    new AiAssessmentCompletedEvent(
                            decode(values[0]),
                            Integer.valueOf(values[1]),
                            Integer.valueOf(values[2]),
                            Integer.valueOf(values[3]),
                            decode(values[4]),
                            decode(values[5]),
                            Integer.valueOf(values[6])));
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
        attemptOutboxRepository
                .findByEventKey(key)
                .ifPresent(
                        row -> {
                            row.setStatus("DISPATCHED");
                            row.setProcessedAt(LocalDateTime.now());
                            row.setLastError(null);
                            attemptOutboxRepository.save(row);
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

    private String flat(Object... values) {
        return java.util.Arrays.stream(values)
                .map(value -> encode(value == null ? "" : value.toString()))
                .collect(java.util.stream.Collectors.joining("|"));
    }

    private String encode(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}

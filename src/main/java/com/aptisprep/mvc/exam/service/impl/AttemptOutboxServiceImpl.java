package com.aptisprep.mvc.exam.service.impl;

import com.aptisprep.mvc.common.event.AiAssessmentCompletedEvent;
import com.aptisprep.mvc.common.event.AttemptCompletedEvent;
import com.aptisprep.mvc.common.event.AttemptSubmittedEvent;
import com.aptisprep.mvc.exam.entity.AttemptOutboxEntity;
import com.aptisprep.mvc.exam.repository.AttemptOutboxRepository;
import com.aptisprep.mvc.exam.repository.ExamAttemptRepository;
import com.aptisprep.mvc.exam.service.AttemptOutboxService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Durable replay queue for attempt events. Handlers are idempotent by attempt/event keys. */
@Service
public class AttemptOutboxServiceImpl implements AttemptOutboxService {
    private final AttemptOutboxRepository attemptOutboxRepository;
    private final ApplicationEventPublisher events;
    private final ExamAttemptRepository attempts;
    private final ExamGradingPayloadFactory gradingPayloads;
    private final JsonMapper json;

    public AttemptOutboxServiceImpl(
            AttemptOutboxRepository attemptOutboxRepository,
            ApplicationEventPublisher events,
            ExamAttemptRepository attempts,
            ExamGradingPayloadFactory gradingPayloads,
            JsonMapper json) {
        this.attemptOutboxRepository = attemptOutboxRepository;
        this.events = events;
        this.attempts = attempts;
        this.gradingPayloads = gradingPayloads;
        this.json = json;
    }

    @Override
    @Transactional
    public void enqueueSubmitted(AttemptSubmittedEvent event) {
        enqueue(
                "attempt:" + event.attemptId() + ":submitted",
                "ATTEMPT_SUBMITTED",
                event.attemptId(),
                flat(event.userId(), event.attemptId(), event.examId(), event.examType()));
    }

    @Override
    @Transactional
    public void enqueueCompleted(AttemptCompletedEvent event) {
        enqueue(
                "attempt:" + event.attemptId() + ":completed",
                "ATTEMPT_COMPLETED",
                event.attemptId(),
                event.userId() + "|" + event.attemptId() + "|" + event.examType());
    }

    @Override
    @Transactional
    public void enqueueAiAssessmentCompleted(AiAssessmentCompletedEvent event) {
        saveAiEvent(event, true);
    }

    /**
     * Backfills callback delivery for completed jobs created before the AI outbox was introduced.
     */
    @Override
    @Transactional
    public void ensureAiAssessmentCompleted(AiAssessmentCompletedEvent event) {
        saveAiEvent(event, false);
    }

    @Override
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
        String payload = row.getPayload();
        boolean legacyJson = payload.stripLeading().startsWith("{");
        if ("ATTEMPT_SUBMITTED".equals(row.getEventType())) {
            if (legacyJson) {
                events.publishEvent(json.readValue(payload, AttemptSubmittedEvent.class));
                return;
            }
            String[] values = payload.split("\\|", -1);
            if (Character.isDigit(values[0].charAt(0))) {
                // The original delimited format stored IDs as plain decimal strings.
                events.publishEvent(
                        new AttemptSubmittedEvent(
                                Integer.valueOf(values[0]),
                                Integer.valueOf(values[1]),
                                Integer.valueOf(values[2]),
                                values[3],
                                null));
                return;
            }
            Integer attemptId = Integer.valueOf(decode(values[1]));
            var attempt = attempts.findById(attemptId).orElseThrow();
            events.publishEvent(
                    new AttemptSubmittedEvent(
                            Integer.valueOf(decode(values[0])),
                            attemptId,
                            Integer.valueOf(decode(values[2])),
                            decode(values[3]),
                            gradingPayloads.build(attempt)));
        } else if ("ATTEMPT_COMPLETED".equals(row.getEventType())) {
            String[] values = payload.split("\\|", -1);
            events.publishEvent(
                    new AttemptCompletedEvent(
                            Integer.valueOf(values[0]), Integer.valueOf(values[1]), values[2]));
        } else if ("AI_ASSESSMENT_COMPLETED".equals(row.getEventType())) {
            if (legacyJson) {
                events.publishEvent(json.readValue(payload, AiAssessmentCompletedEvent.class));
                return;
            }
            String[] values = payload.split("\\|", -1);
            events.publishEvent(
                    new AiAssessmentCompletedEvent(
                            decode(values[0]),
                            Integer.valueOf(decode(values[1])),
                            Integer.valueOf(decode(values[2])),
                            Integer.valueOf(decode(values[3])),
                            decode(values[4]),
                            decode(values[5]),
                            Integer.valueOf(decode(values[6]))));
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDelivered(AttemptSubmittedEvent event) {
        markDeliveredKey("attempt:" + event.attemptId() + ":submitted");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDelivered(AttemptCompletedEvent event) {
        markDeliveredKey("attempt:" + event.attemptId() + ":completed");
    }

    @Override
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

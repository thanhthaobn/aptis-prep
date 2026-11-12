package com.aptisprep.mvc.service.exam.impl;

import com.aptisprep.mvc.entity.AttemptOutbox;
import com.aptisprep.mvc.event.AiAssessmentCompletedEvent;
import com.aptisprep.mvc.event.AttemptCompletedEvent;
import com.aptisprep.mvc.event.AttemptSubmittedEvent;
import com.aptisprep.mvc.repository.AttemptOutboxRepository;
import com.aptisprep.mvc.repository.ExamAttemptRepository;
import com.aptisprep.mvc.service.exam.*;
import com.aptisprep.mvc.service.exam.AttemptOutboxService;
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

/** Hàng đợi replay bền vững cho event attempt. Handler idempotent theo khóa attempt/event. */
@Service
public class AttemptOutboxServiceImpl implements AttemptOutboxService {
    private AttemptOutboxRepository attemptOutboxRepository;
    private ApplicationEventPublisher events;
    private ExamAttemptRepository attempts;
    private ExamGradingPayloadService gradingPayloads;
    private JsonMapper json;

    public AttemptOutboxServiceImpl(
            AttemptOutboxRepository attemptOutboxRepository,
            ApplicationEventPublisher events,
            ExamAttemptRepository attempts,
            ExamGradingPayloadService gradingPayloads,
            JsonMapper json) {
        this.attemptOutboxRepository = attemptOutboxRepository;
        this.events = events;
        this.attempts = attempts;
        this.gradingPayloads = gradingPayloads;
        this.json = json;
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
     * Bổ sung việc giao callback cho các job đã hoàn tất được tạo trước khi có AI outbox.
     */
    @Transactional
    public void ensureAiAssessmentCompleted(AiAssessmentCompletedEvent event) {
        saveAiEvent(event, false);
    }

    @Scheduled(fixedDelayString = "${app.attempt-outbox.recovery-ms:30000}")
    @Transactional
    public void recoverPending() {
        List<AttemptOutbox> pending =
                attemptOutboxRepository
                        .findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(
                                List.of("PENDING", "PROCESSING"), LocalDateTime.now());
        for (AttemptOutbox row : pending) {
            try {
                row.setStatus("PROCESSING");
                row.setAttempts(row.getAttempts() + 1);
                // Lưu retry lease trước khi publish: lỗi listener sau commit không thể bắt tại đây
                // và không được chiếm cố định trang đầu tiên.
                row.setNextAttemptAt(LocalDateTime.now().plusSeconds(backoff(row.getAttempts())));
                attemptOutboxRepository.save(row);
                dispatch(row);
                // Event listener chạy sau khi transaction này commit. Giữ row ở PROCESSING
                // đến khi marker giao event được commit để crash giữa lúc publish và chạy listener
                // vẫn có thể khôi phục.
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
        AttemptOutbox row = new AttemptOutbox();
        row.setEventKey(key);
        row.setEventType(type);
        row.setAggregateId(aggregateId);
        row.setPayload(payload);
        row.setStatus("PENDING");
        attemptOutboxRepository.save(row);
    }

    private void saveAiEvent(AiAssessmentCompletedEvent event, boolean replaceExisting) {
        String key = aiEventKey(event);
        AttemptOutbox row =
                attemptOutboxRepository.findByEventKey(key).orElseGet(() -> new AttemptOutbox());
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

    private void dispatch(AttemptOutbox row) {
        String payload = row.getPayload();
        boolean legacyJson = payload.stripLeading().startsWith("{");
        if ("ATTEMPT_SUBMITTED".equals(row.getEventType())) {
            if (legacyJson) {
                events.publishEvent(json.readValue(payload, AttemptSubmittedEvent.class));
                return;
            }
            String[] values = payload.split("\\|", -1);
            if (Character.isDigit(values[0].charAt(0))) {
                // Format phân cách ban đầu lưu ID dưới dạng chuỗi thập phân thông thường.
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

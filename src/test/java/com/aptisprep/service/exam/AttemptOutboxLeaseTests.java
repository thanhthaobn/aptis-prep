package com.aptisprep.service.exam;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aptisprep.entity.*;
import com.aptisprep.entity.AttemptOutboxEntity;
import com.aptisprep.event.AttemptCompletedEvent;
import com.aptisprep.repository.*;
import com.aptisprep.repository.AttemptOutboxRepository;
import com.aptisprep.repository.ExamAttemptRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class AttemptOutboxLeaseTests {
    @Test
    void retryLeaseIsSavedBeforePublicationWhenTheDeliveryMarkerIsLost() {
        var repository = mock(AttemptOutboxRepository.class);
        var publisher = mock(ApplicationEventPublisher.class);
        var row = new AttemptOutboxEntity();
        row.setEventKey("attempt:1:completed");
        row.setEventType("ATTEMPT_COMPLETED");
        row.setAggregateId(1);
        row.setPayload("2|1|PLACEMENT");
        row.setStatus("PENDING");
        row.setNextAttemptAt(LocalDateTime.now().minusMinutes(1));
        when(repository.findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(any(), any()))
                .thenReturn(List.of(row));
        doAnswer(
                        call -> {
                            assertEquals("PROCESSING", row.getStatus());
                            assertTrue(row.getNextAttemptAt().isAfter(LocalDateTime.now()));
                            return null; // Simulate a restart before the consumer can mark
                            // delivery.
                        })
                .when(publisher)
                .publishEvent(any(AttemptCompletedEvent.class));
        new com.aptisprep.service.exam.impl.AttemptOutboxServiceImpl(
                        repository, publisher, mock(ExamAttemptRepository.class), null, null)
                .recoverPending();
        assertEquals("PROCESSING", row.getStatus());
        assertEquals(1, row.getAttempts());
        assertTrue(row.getNextAttemptAt().isAfter(LocalDateTime.now()));
    }
}

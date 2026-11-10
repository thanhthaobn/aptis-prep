package com.aptisprep.mvc.exam.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.aptisprep.mvc.common.config.AiProperties;
import com.aptisprep.mvc.common.event.AttemptSubmittedEvent;
import com.aptisprep.mvc.exam.dto.LockedAnswerDTO;
import com.aptisprep.mvc.exam.dto.LockedAttemptDTO;
import com.aptisprep.mvc.exam.dto.LockedItemDTO;
import com.aptisprep.mvc.exam.service.impl.AttemptGradingOrchestrationListener;
import com.aptisprep.mvc.grading.dto.AiAssessmentDTO.AssessmentRequest;
import com.aptisprep.mvc.grading.service.AiAssessmentService;
import com.aptisprep.mvc.grading.service.AttemptFinalizationService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AttemptGradingOrchestrationListenerTests {
    @Test
    void sendsEveryItemPromptAndKeepsEachAnswerWithItsItem() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        AiAssessmentService ai = mock(AiAssessmentService.class);
        AttemptFinalizationService finalization = mock(AttemptFinalizationService.class);
        when(finalization.accepts(any())).thenReturn(true);
        AttemptGradingOrchestrationListener service =
                new AttemptGradingOrchestrationListener(
                        properties, ai, finalization, mock(AttemptOutboxService.class));

        LockedAnswerDTO answer =
                new LockedAnswerDTO(
                        11,
                        21,
                        "WRITING",
                        "WRITING_TEXT",
                        "Write two messages",
                        "first answer\n--- ITEM ---\nsecond answer",
                        null,
                        List.of(
                                new LockedItemDTO(
                                        31,
                                        1,
                                        "Write to Ana about the meeting",
                                        "first answer",
                                        null,
                                        null),
                                new LockedItemDTO(
                                        32,
                                        2,
                                        "Write to Ben about the timetable",
                                        "second answer",
                                        null,
                                        null)));
        LockedAttemptDTO locked =
                new LockedAttemptDTO(
                        7,
                        2,
                        9,
                        "PRACTICE",
                        4,
                        Instant.now(),
                        5,
                        "policy-v2",
                        "rubric-v2",
                        "snapshot-v1",
                        "attempt-key",
                        List.of(answer));

        service.onSubmitted(new AttemptSubmittedEvent(2, 7, 9, "PRACTICE", locked));

        ArgumentCaptor<AssessmentRequest> captor = ArgumentCaptor.forClass(AssessmentRequest.class);
        verify(ai).request(captor.capture());
        AssessmentRequest request = captor.getValue();
        assertTrue(
                request.taskContent().indexOf("Mục 1:\nWrite to Ana")
                        < request.taskContent().indexOf("Mục 2:\nWrite to Ben"));
        assertTrue(request.lockedContent().contains("Mục 1:\nfirst answer"));
        assertTrue(request.lockedContent().contains("Mục 2:\nsecond answer"));
        verify(ai).process("attempt:7:answer:11:v4");
    }

    @Test
    void learnerQuestionViewHasNoAnswerKeyFields() {
        var view =
                new com.aptisprep.mvc.catalog.dto.QuestionViewDTO(
                        21, "WRITING", "WRITING_TEXT", 1, "Write a message");
        String serializedShape =
                view.getClass().getRecordComponents() == null
                        ? ""
                        : java.util.Arrays.stream(view.getClass().getRecordComponents())
                                .map(java.lang.reflect.RecordComponent::getName)
                                .reduce("", (a, b) -> a + b);
        assertTrue(!serializedShape.contains("correct") && !serializedShape.contains("answer"));
    }
}

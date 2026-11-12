package com.aptisprep.mvc.service.exam;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.aptisprep.mvc.config.AiProperties;
import com.aptisprep.mvc.dto.AiAssessmentJobDTO;
import com.aptisprep.mvc.event.AttemptSubmittedEvent;
import com.aptisprep.mvc.service.exam.impl.AttemptGradingOrchestrationServiceImpl;
import com.aptisprep.mvc.service.grading.AiAssessmentService;
import com.aptisprep.mvc.service.grading.AttemptFinalizationService;
import com.aptisprep.mvc.support.contract.*;
import com.aptisprep.mvc.support.contract.IntegrationModels.LockedAnswer;
import com.aptisprep.mvc.support.contract.IntegrationModels.LockedAttempt;
import com.aptisprep.mvc.support.contract.IntegrationModels.LockedItem;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AttemptGradingOrchestrationServiceTests {
    @Test
    void sendsEveryItemPromptAndKeepsEachAnswerWithItsItem() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        AiAssessmentService ai = mock(AiAssessmentService.class);
        AttemptFinalizationService finalization = mock(AttemptFinalizationService.class);
        when(finalization.accepts(any())).thenReturn(true);
        AttemptGradingOrchestrationService service =
                new AttemptGradingOrchestrationServiceImpl(
                        properties, ai, finalization, mock(AttemptOutboxService.class));

        LockedAnswer answer =
                new LockedAnswer(
                        11,
                        21,
                        "WRITING",
                        "WRITING_TEXT",
                        "Write two messages",
                        "first answer\n--- ITEM ---\nsecond answer",
                        null,
                        List.of(
                                new LockedItem(
                                        31,
                                        1,
                                        "Write to Ana about the meeting",
                                        "first answer",
                                        null,
                                        null),
                                new LockedItem(
                                        32,
                                        2,
                                        "Write to Ben about the timetable",
                                        "second answer",
                                        null,
                                        null)));
        LockedAttempt locked =
                new LockedAttempt(
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

        ArgumentCaptor<AiAssessmentJobDTO> captor =
                ArgumentCaptor.forClass(AiAssessmentJobDTO.class);
        verify(ai).request(captor.capture());
        AiAssessmentJobDTO request = captor.getValue();
        assertTrue(
                request.getTaskContent().indexOf("Mục 1:\nWrite to Ana")
                        < request.getTaskContent().indexOf("Mục 2:\nWrite to Ben"));
        assertTrue(request.getLockedContent().contains("Mục 1:\nfirst answer"));
        assertTrue(request.getLockedContent().contains("Mục 2:\nsecond answer"));
        verify(ai).process("attempt:7:answer:11:v4");
    }

    @Test
    void learnerQuestionViewHasNoAnswerKeyFields() {
        var view =
                new com.aptisprep.mvc.support.contract.IntegrationModels.QuestionView(
                        21, "WRITING", "WRITING_TEXT", 1, "Write a message");
        String serializedShape =
                view.getClass().getRecordComponents() == null
                        ? ""
                        : java.util.Arrays.stream(view.getClass().getRecordComponents())
                                .map(streamValue -> streamValue.getName())
                                .reduce("", (a, b) -> a + b);
        assertTrue(!serializedShape.contains("correct") && !serializedShape.contains("answer"));
    }
}

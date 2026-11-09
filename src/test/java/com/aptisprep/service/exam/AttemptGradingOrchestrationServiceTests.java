package com.aptisprep.service.exam;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.aptisprep.config.AiProperties;
import com.aptisprep.event.AttemptSubmittedEvent;
import com.aptisprep.service.contract.AiAssessmentContract;
import com.aptisprep.service.contract.AiAssessmentContract.AssessmentRequest;
import com.aptisprep.service.contract.IntegrationModels.LockedAnswer;
import com.aptisprep.service.contract.IntegrationModels.LockedAttempt;
import com.aptisprep.service.contract.IntegrationModels.LockedItem;
import com.aptisprep.service.grading.AttemptFinalizationService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AttemptGradingOrchestrationServiceTests {
    @Test
    void sendsEveryItemPromptAndKeepsEachAnswerWithItsItem() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        AiAssessmentContract ai = mock(AiAssessmentContract.class);
        AttemptFinalizationService finalization = mock(AttemptFinalizationService.class);
        when(finalization.accepts(any())).thenReturn(true);
        AttemptGradingOrchestrationService service =
                new AttemptGradingOrchestrationService(
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
                new com.aptisprep.service.contract.IntegrationModels.QuestionView(
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

package com.aptisprep.service.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aptisprep.entity.*;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.repository.*;
import com.aptisprep.service.exam.impl.ExamStartValidator;
import com.aptisprep.service.scoring.*;
import com.aptisprep.service.scoring.CertificateSchemeService;
import com.aptisprep.service.storage.StorageService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ExamPublicationReadinessTests {
    @Test
    void publicationChecksDraftContentAndAllowsBrowserSpeechWhenListeningAudioIsMissing() {
        var catalogService = mock(ExamCatalogService.class);
        var storage = mock(StorageService.class);
        var validator =
                new ExamStartValidator(
                        catalogService, mock(CertificateSchemeService.class), storage);
        var exam = new MockExamEntity();
        ReflectionTestUtils.setField(exam, "id", 1);
        exam.setExamType("PRACTICE");
        exam.setStatus("DRAFT");
        exam.setSkill("READING");
        exam.setDurationMinutes(20);
        var question = new QuestionEntity();
        ReflectionTestUtils.setField(question, "id", 2);
        question.setSkill("READING");
        question.setStatus("PUBLISHED");
        var link = new MockExamQuestionEntity();
        link.setMockExamId(1);
        link.setQuestionId(2);
        when(catalogService.findQuestionById(2)).thenReturn(Optional.of(question));

        assertDoesNotThrow(() -> validator.validateForPublication(exam, List.of(link)));
        exam.setSkill("LISTENING");
        question.setSkill("LISTENING");
        assertThrows(
                BusinessRuleException.class,
                () -> validator.validateForPublication(exam, List.of(link)));

        var audio = new QuestionMediaEntity();
        audio.setMediaType("AUDIO");
        audio.setMediaUrl("questions/missing.wav");
        audio.setTranscript("This is a browser speech fallback.");
        when(catalogService.findQuestionMedia(2)).thenReturn(List.of(audio));
        when(storage.load("questions/missing.wav")).thenThrow(new BusinessRuleException("Missing"));
        assertDoesNotThrow(() -> validator.validateForPublication(exam, List.of(link)));
        assertEquals("DRAFT", exam.getStatus());
    }
}

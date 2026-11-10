package com.aptisprep.service.exam;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aptisprep.entity.*;
import com.aptisprep.mvc.common.exception.BusinessRuleException;
import com.aptisprep.mvc.scoring.service.*;
import com.aptisprep.mvc.scoring.service.CertificateSchemeService;
import com.aptisprep.mvc.storage.service.StorageService;
import com.aptisprep.repository.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ExamPublicationReadinessTests {
    @Test
    void publicationChecksDraftContentAndAllowsBrowserSpeechWhenListeningAudioIsMissing() {
        var questions = mock(QuestionRepository.class);
        var sections = mock(MockExamSectionRepository.class);
        var media = mock(QuestionMediaRepository.class);
        var storage = mock(StorageService.class);
        var validator =
                new ExamStartValidator(
                        questions, sections, media, mock(CertificateSchemeService.class), storage);
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
        when(questions.findById(2)).thenReturn(Optional.of(question));

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
        when(media.findByQuestionIdOrderByPosition(2)).thenReturn(List.of(audio));
        when(storage.load("questions/missing.wav")).thenThrow(new BusinessRuleException("Missing"));
        assertDoesNotThrow(() -> validator.validateForPublication(exam, List.of(link)));
        assertEquals("DRAFT", exam.getStatus());
    }
}

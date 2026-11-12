package com.aptisprep.mvc.service.exam;

import com.aptisprep.mvc.service.exam.impl.ExamReadinessServiceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.repository.*;
import com.aptisprep.mvc.service.scoring.CertificateSchemeService;
import com.aptisprep.mvc.service.storage.StorageService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ExamPublicationReadinessTests {
    @Test
    void publicationChecksDraftContentAndRejectsMissingListeningAudio() {
        var questions = mock(QuestionRepository.class);
        var sections = mock(MockExamSectionRepository.class);
        var media = mock(QuestionMediaRepository.class);
        var storage = mock(StorageService.class);
        var validator =
                new ExamReadinessServiceImpl(
                        questions, sections, media, mock(CertificateSchemeService.class), storage);
        var exam = new MockExam();
        ReflectionTestUtils.setField(exam, "id", 1);
        exam.setExamType("PRACTICE");
        exam.setStatus("DRAFT");
        exam.setSkill("READING");
        exam.setDurationMinutes(20);
        var question = new Question();
        ReflectionTestUtils.setField(question, "id", 2);
        question.setSkill("READING");
        question.setStatus("PUBLISHED");
        var link = new MockExamQuestion();
        link.setMockExamId(1);
        link.setQuestionId(2);
        when(questions.findById(2)).thenReturn(Optional.of(question));

        assertDoesNotThrow(() -> validator.validateForPublication(exam, List.of(link)));
        exam.setSkill("LISTENING");
        question.setSkill("LISTENING");
        assertThrows(
                BusinessRuleException.class,
                () -> validator.validateForPublication(exam, List.of(link)));

        var audio = new QuestionMedia();
        audio.setMediaType("AUDIO");
        audio.setMediaUrl("questions/missing.wav");
        when(media.findByQuestionIdOrderByPosition(2)).thenReturn(List.of(audio));
        when(storage.load("questions/missing.wav")).thenThrow(new BusinessRuleException("Missing"));
        assertThrows(
                BusinessRuleException.class,
                () -> validator.validateForPublication(exam, List.of(link)));
        assertEquals("DRAFT", exam.getStatus());
    }
}











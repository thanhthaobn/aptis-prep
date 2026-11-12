package com.aptisprep.mvc.service.placement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.aptisprep.mvc.entity.MockExam;
import com.aptisprep.mvc.entity.MockExamQuestion;
import com.aptisprep.mvc.repository.MockExamQuestionRepository;
import com.aptisprep.mvc.repository.MockExamRepository;
import com.aptisprep.mvc.repository.MockExamSectionRepository;
import com.aptisprep.mvc.service.placement.impl.PlacementExamCatalogServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlacementExamCatalogServiceTests {
    @Test
    void onlyPublishedPlacementExamsWithPublishedQuestionsAreAvailable() {
        MockExamRepository exams = mock(MockExamRepository.class);
        MockExamQuestionRepository examQuestions = mock(MockExamQuestionRepository.class);
        var validator = mock(com.aptisprep.mvc.service.exam.ExamReadinessService.class);
        PlacementExamCatalogService service =
                new PlacementExamCatalogServiceImpl(
                        exams, examQuestions, mock(MockExamSectionRepository.class), validator);

        MockExam placement = exam("placement", "PLACEMENT", 101);
        MockExam practice = exam("practice", "PRACTICE", 102);
        MockExam empty = exam("empty", "PLACEMENT", 103);
        MockExamQuestion link = link(101, 201);

        when(exams.findBySkillAndStatusOrderByUpdatedAtDesc("LISTENING", "PUBLISHED"))
                .thenReturn(List.of(placement, practice, empty));
        when(examQuestions.findByMockExamIdOrderByPosition(101)).thenReturn(List.of(link));
        when(examQuestions.findByMockExamIdOrderByPosition(103)).thenReturn(List.of());
        doThrow(new com.aptisprep.mvc.exception.BusinessRuleException("Empty exam"))
                .when(validator)
                .validate(eq(empty), anyList());

        var available = service.findPublished(" listening ");

        assertEquals(
                List.of(101), available.stream().map(streamValue -> streamValue.id()).toList());
        verify(examQuestions, never()).findByMockExamIdOrderByPosition(102);
    }

    private MockExam exam(String title, String type, int id) {
        MockExam exam = mock(MockExam.class);
        when(exam.getId()).thenReturn(id);
        when(exam.getTitle()).thenReturn(title);
        when(exam.getExamType()).thenReturn(type);
        when(exam.getDescription()).thenReturn("description");
        when(exam.getDurationMinutes()).thenReturn(20);
        return exam;
    }

    private MockExamQuestion link(int examId, int questionId) {
        MockExamQuestion link = mock(MockExamQuestion.class);
        when(link.getMockExamId()).thenReturn(examId);
        when(link.getQuestionId()).thenReturn(questionId);
        return link;
    }
}

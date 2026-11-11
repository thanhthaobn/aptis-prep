package com.aptisprep.service.exam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.entity.MockExamQuestionEntity;
import com.aptisprep.presentation.PlacementDTO;
import com.aptisprep.repository.MockExamQuestionRepository;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.repository.MockExamSectionRepository;
import com.aptisprep.service.exam.impl.PlacementExamCatalogServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlacementExamCatalogServiceTests {
    @Test
    void onlyPublishedPlacementExamsWithPublishedQuestionsAreAvailable() {
        MockExamRepository exams = mock(MockExamRepository.class);
        MockExamQuestionRepository examQuestions = mock(MockExamQuestionRepository.class);
        var validator = mock(com.aptisprep.service.exam.impl.ExamStartValidator.class);
        var catalogService =
                new com.aptisprep.service.admin.impl.ExamCatalogServiceImpl(
                        exams,
                        examQuestions,
                        mock(MockExamSectionRepository.class),
                        null,
                        null,
                        null,
                        null);
        PlacementExamCatalogService service =
                new PlacementExamCatalogServiceImpl(catalogService, validator);

        MockExamEntity placement = exam("placement", "PLACEMENT", 101);
        MockExamEntity practice = exam("practice", "PRACTICE", 102);
        MockExamEntity empty = exam("empty", "PLACEMENT", 103);
        MockExamQuestionEntity link = link(101, 201);

        when(exams.findBySkillAndStatusOrderByUpdatedAtDesc("LISTENING", "PUBLISHED"))
                .thenReturn(List.of(placement, practice, empty));
        when(examQuestions.findByMockExamIdOrderByPosition(101)).thenReturn(List.of(link));
        when(examQuestions.findByMockExamIdOrderByPosition(103)).thenReturn(List.of());
        doThrow(new com.aptisprep.exception.BusinessRuleException("Empty exam"))
                .when(validator)
                .validate(eq(empty), anyList());

        var available = service.findPublished(" listening ");

        assertEquals(List.of(101), available.stream().map(PlacementDTO.PlacementExam::id).toList());
        verify(examQuestions, never()).findByMockExamIdOrderByPosition(102);
    }

    private MockExamEntity exam(String title, String type, int id) {
        MockExamEntity exam = mock(MockExamEntity.class);
        when(exam.getId()).thenReturn(id);
        when(exam.getTitle()).thenReturn(title);
        when(exam.getExamType()).thenReturn(type);
        when(exam.getDescription()).thenReturn("description");
        when(exam.getDurationMinutes()).thenReturn(20);
        return exam;
    }

    private MockExamQuestionEntity link(int examId, int questionId) {
        MockExamQuestionEntity link = mock(MockExamQuestionEntity.class);
        when(link.getMockExamId()).thenReturn(examId);
        when(link.getQuestionId()).thenReturn(questionId);
        return link;
    }
}

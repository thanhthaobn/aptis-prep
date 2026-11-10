package com.aptisprep.service.placement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.aptisprep.mvc.catalog.entity.*;
import com.aptisprep.mvc.catalog.entity.MockExamEntity;
import com.aptisprep.mvc.catalog.entity.MockExamQuestionEntity;
import com.aptisprep.mvc.catalog.repository.*;
import com.aptisprep.mvc.catalog.repository.MockExamQuestionRepository;
import com.aptisprep.mvc.catalog.repository.MockExamRepository;
import com.aptisprep.mvc.catalog.repository.MockExamSectionRepository;
import com.aptisprep.mvc.catalog.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlacementExamCatalogServiceTests {
    @Test
    void onlyPublishedPlacementExamsWithPublishedQuestionsAreAvailable() {
        MockExamRepository exams = mock(MockExamRepository.class);
        MockExamQuestionRepository examQuestions = mock(MockExamQuestionRepository.class);
        var validator = mock(com.aptisprep.mvc.exam.service.impl.ExamStartValidator.class);
        PlacementExamCatalogService service =
                new PlacementExamCatalogService(
                        exams, examQuestions, mock(MockExamSectionRepository.class), validator);

        MockExamEntity placement = exam("placement", "PLACEMENT", 101);
        MockExamEntity practice = exam("practice", "PRACTICE", 102);
        MockExamEntity empty = exam("empty", "PLACEMENT", 103);
        MockExamQuestionEntity link = link(101, 201);

        when(exams.findBySkillAndStatusOrderByUpdatedAtDesc("LISTENING", "PUBLISHED"))
                .thenReturn(List.of(placement, practice, empty));
        when(examQuestions.findByMockExamIdOrderByPosition(101)).thenReturn(List.of(link));
        when(examQuestions.findByMockExamIdOrderByPosition(103)).thenReturn(List.of());
        doThrow(new com.aptisprep.mvc.common.exception.BusinessRuleException("Empty exam"))
                .when(validator)
                .validate(eq(empty), anyList());

        var available = service.findPublished(" listening ");

        assertEquals(
                List.of(101),
                available.stream().map(PlacementExamCatalogService.PlacementExam::id).toList());
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

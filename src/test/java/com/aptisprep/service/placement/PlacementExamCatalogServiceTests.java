package com.aptisprep.service.placement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.entity.MockExamQuestionEntity;
import com.aptisprep.entity.QuestionEntity;
import com.aptisprep.repository.MockExamQuestionRepository;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.repository.QuestionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlacementExamCatalogServiceTests {
    @Test
    void onlyPublishedPlacementExamsWithPublishedQuestionsAreAvailable() {
        MockExamRepository exams = mock(MockExamRepository.class);
        MockExamQuestionRepository examQuestions = mock(MockExamQuestionRepository.class);
        QuestionRepository questions = mock(QuestionRepository.class);
        PlacementExamCatalogService service = new PlacementExamCatalogService(exams, examQuestions, questions);

        MockExamEntity placement = exam("placement", "PLACEMENT", 101);
        MockExamEntity practice = exam("practice", "PRACTICE", 102);
        MockExamEntity empty = exam("empty", "PLACEMENT", 103);
        QuestionEntity published = question("PUBLISHED");
        MockExamQuestionEntity link = link(101, 201);

        when(exams.findBySkillAndStatusOrderByUpdatedAtDesc("LISTENING", "PUBLISHED"))
                .thenReturn(List.of(placement, practice, empty));
        when(examQuestions.findByMockExamIdOrderByPosition(101)).thenReturn(List.of(link));
        when(examQuestions.findByMockExamIdOrderByPosition(103)).thenReturn(List.of());
        when(questions.findById(201)).thenReturn(Optional.of(published));

        var available = service.findPublished(" listening ");

        assertEquals(List.of(101), available.stream().map(PlacementExamCatalogService.PlacementExam::id).toList());
        verify(examQuestions, never()).findByMockExamIdOrderByPosition(102);
    }

    private MockExamEntity exam(String title, String type, int id) {
        MockExamEntity exam = mock(MockExamEntity.class);
        when(exam.getId()).thenReturn(id);
        when(exam.getTitle()).thenReturn(title);
        when(exam.getExamType()).thenReturn(type);
        when(exam.getDescription()).thenReturn("description");
        return exam;
    }

    private QuestionEntity question(String status) {
        QuestionEntity question = mock(QuestionEntity.class);
        when(question.getStatus()).thenReturn(status);
        return question;
    }

    private MockExamQuestionEntity link(int examId, int questionId) {
        MockExamQuestionEntity link = mock(MockExamQuestionEntity.class);
        when(link.getMockExamId()).thenReturn(examId);
        when(link.getQuestionId()).thenReturn(questionId);
        return link;
    }
}

package com.aptisprep.service.placement;

import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.repository.MockExamQuestionRepository;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.repository.QuestionRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Read model for Placement only. It deliberately does not expose the admin catalog model. */
@Service
public class PlacementExamCatalogService {
    private final MockExamRepository exams;
    private final MockExamQuestionRepository examQuestions;
    private final QuestionRepository questions;

    public PlacementExamCatalogService(MockExamRepository exams, MockExamQuestionRepository examQuestions,
            QuestionRepository questions) {
        this.exams = exams;
        this.examQuestions = examQuestions;
        this.questions = questions;
    }

    public List<PlacementExam> findPublished(String skill) {
        String normalized = skill == null ? "" : skill.trim().toUpperCase(Locale.ROOT);
        return exams.findBySkillAndStatusOrderByUpdatedAtDesc(normalized, "PUBLISHED").stream()
                .filter(exam -> !exam.isDemoOnly())
                .filter(exam -> "PLACEMENT".equals(exam.getExamType()))
                .filter(this::hasPublishedQuestions)
                .map(exam -> new PlacementExam(exam.getId(), exam.getTitle(), exam.getDescription()))
                .toList();
    }

    private boolean hasPublishedQuestions(MockExamEntity exam) {
        List<Integer> questionIds = examQuestions.findByMockExamIdOrderByPosition(exam.getId()).stream()
                .map(link -> link.getQuestionId())
                .toList();
        return !questionIds.isEmpty() && questionIds.stream()
                .map(questions::findById)
                .allMatch(value -> value.filter(question -> "PUBLISHED".equals(question.getStatus())).isPresent());
    }

    public record PlacementExam(Integer id, String title, String subtitle) {
    }
}

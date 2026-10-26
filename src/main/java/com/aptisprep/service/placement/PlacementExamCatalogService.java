package com.aptisprep.service.placement;

import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.enums.Skill;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.repository.MockExamQuestionRepository;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.service.exam.ExamStartValidator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Read model for Placement only. It deliberately does not expose the admin catalog model. */
@Service
public class PlacementExamCatalogService {
    private final MockExamRepository exams;
    private final MockExamQuestionRepository examQuestions;
    private final ExamStartValidator startValidator;

    public PlacementExamCatalogService(
            MockExamRepository exams,
            MockExamQuestionRepository examQuestions,
            ExamStartValidator startValidator) {
        this.exams = exams;
        this.examQuestions = examQuestions;
        this.startValidator = startValidator;
    }

    public List<PlacementExam> findPublished(String skill) {
        String normalized = skill == null ? "" : skill.trim().toUpperCase(Locale.ROOT);
        if (!Skill.codes().contains(normalized))
            throw new BusinessRuleException("Kỹ năng Placement không hợp lệ");
        return exams.findBySkillAndStatusOrderByUpdatedAtDesc(normalized, "PUBLISHED").stream()
                .filter(exam -> !exam.isDemoOnly())
                .filter(exam -> "PLACEMENT".equals(exam.getExamType()))
                .filter(this::isReady)
                .map(
                        exam ->
                                new PlacementExam(
                                        exam.getId(), exam.getTitle(), exam.getDescription()))
                .toList();
    }

    private boolean isReady(MockExamEntity exam) {
        try {
            startValidator.validate(
                    exam, examQuestions.findByMockExamIdOrderByPosition(exam.getId()));
            return true;
        } catch (BusinessRuleException exception) {
            return false;
        }
    }

    public record PlacementExam(Integer id, String title, String subtitle) {}
}

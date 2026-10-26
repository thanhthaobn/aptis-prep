package com.aptisprep.service.catalog;

import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.MockExamQuestionRepository;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.repository.QuestionRepository;
import com.aptisprep.service.contract.ExamCatalogContract;
import com.aptisprep.service.contract.IntegrationModels.ExamSummary;
import com.aptisprep.service.contract.IntegrationModels.ExamView;
import com.aptisprep.service.contract.IntegrationModels.QuestionView;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PublishedExamCatalogService implements ExamCatalogContract {
    private final MockExamRepository mockExamRepository;
    private final MockExamQuestionRepository mockExamQuestionRepository;
    private final QuestionRepository questionRepository;

    public PublishedExamCatalogService(
            MockExamRepository mockExamRepository,
            MockExamQuestionRepository mockExamQuestionRepository,
            QuestionRepository questionRepository) {
        this.mockExamRepository = mockExamRepository;
        this.mockExamQuestionRepository = mockExamQuestionRepository;
        this.questionRepository = questionRepository;
    }

    @Override
    public ExamView publishedExam(Integer userId, Integer examId) {
        MockExamEntity exam =
                mockExamRepository
                        .findById(examId)
                        .filter(x -> "PUBLISHED".equals(x.getStatus()) && !x.isDemoOnly())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Đề thi chưa được công bố hoặc không tồn tại"));
        ExamSummary summary = summary(exam);
        List<QuestionView> views =
                mockExamQuestionRepository.findByMockExamIdOrderByPosition(examId).stream()
                        .map(
                                link ->
                                        questionRepository
                                                .findById(link.getQuestionId())
                                                .filter(q -> "PUBLISHED".equals(q.getStatus()))
                                                .map(
                                                        q ->
                                                                new QuestionView(
                                                                        q.getId(),
                                                                        q.getSkill(),
                                                                        q.getQuestionType(),
                                                                        link.getPosition(),
                                                                        q.getContent()))
                                                .orElseThrow(
                                                        () ->
                                                                new ResourceNotFoundException(
                                                                        "Đề thi chứa câu hỏi chưa công bố")))
                        .toList();
        if (views.isEmpty()) throw new ResourceNotFoundException("Đề thi chưa có câu hỏi");
        return new ExamView(summary, views);
    }

    @Override
    public ExamSummary checkpoint(String skill, String targetLevel) {
        return mockExamRepository.findByStatusOrderByUpdatedAtDesc("PUBLISHED").stream()
                .filter(x -> !x.isDemoOnly())
                .filter(
                        x ->
                                "CHECKPOINT".equals(x.getExamType())
                                        && skill.equals(x.getSkill())
                                        && targetLevel.equals(x.getTargetLevel()))
                .filter(
                        x ->
                                !mockExamQuestionRepository
                                        .findByMockExamIdOrderByPosition(x.getId())
                                        .isEmpty())
                .findFirst()
                .map(this::summary)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có đề checkpoint phù hợp"));
    }

    private ExamSummary summary(MockExamEntity x) {
        return new ExamSummary(
                x.getId(),
                x.getTitle(),
                x.getExamType(),
                x.getSkill(),
                x.getTargetLevel(),
                x.getDurationMinutes(),
                x.getScoringSchemeId());
    }
}

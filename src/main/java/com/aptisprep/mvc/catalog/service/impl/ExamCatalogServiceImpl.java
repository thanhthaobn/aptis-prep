package com.aptisprep.mvc.catalog.service.impl;

import com.aptisprep.mvc.catalog.entity.MockExamEntity;
import com.aptisprep.mvc.catalog.repository.MockExamQuestionRepository;
import com.aptisprep.mvc.catalog.repository.MockExamRepository;
import com.aptisprep.mvc.catalog.repository.QuestionRepository;
import com.aptisprep.mvc.catalog.service.ExamCatalogService;
import com.aptisprep.mvc.common.exception.ResourceNotFoundException;
import com.aptisprep.service.contract.IntegrationModels.ExamSummary;
import com.aptisprep.service.contract.IntegrationModels.ExamView;
import com.aptisprep.service.contract.IntegrationModels.QuestionView;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ExamCatalogServiceImpl implements ExamCatalogService {
    private final MockExamRepository mockExamRepository;
    private final MockExamQuestionRepository mockExamQuestionRepository;
    private final QuestionRepository questionRepository;

    public ExamCatalogServiceImpl(
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

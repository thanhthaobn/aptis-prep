package com.aptisprep.mvc.catalog.service.impl;

import com.aptisprep.mvc.catalog.dto.ExamSummaryDTO;
import com.aptisprep.mvc.catalog.dto.ExamViewDTO;
import com.aptisprep.mvc.catalog.dto.QuestionViewDTO;
import com.aptisprep.mvc.catalog.entity.*;
import com.aptisprep.mvc.catalog.repository.*;
import com.aptisprep.mvc.catalog.service.ExamCatalogService;
import com.aptisprep.mvc.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamCatalogServiceImpl implements ExamCatalogService {
    private final MockExamRepository mockExamRepository;
    private final MockExamQuestionRepository mockExamQuestionRepository;
    private final MockExamSectionRepository mockExamSectionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionItemRepository questionItemRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final QuestionMediaRepository questionMediaRepository;

    public ExamCatalogServiceImpl(
            MockExamRepository mockExamRepository,
            MockExamQuestionRepository mockExamQuestionRepository,
            MockExamSectionRepository mockExamSectionRepository,
            QuestionRepository questionRepository,
            QuestionItemRepository questionItemRepository,
            QuestionOptionRepository questionOptionRepository,
            QuestionMediaRepository questionMediaRepository) {
        this.mockExamRepository = mockExamRepository;
        this.mockExamQuestionRepository = mockExamQuestionRepository;
        this.mockExamSectionRepository = mockExamSectionRepository;
        this.questionRepository = questionRepository;
        this.questionItemRepository = questionItemRepository;
        this.questionOptionRepository = questionOptionRepository;
        this.questionMediaRepository = questionMediaRepository;
    }

    @Override
    public Optional<MockExamEntity> findMockExamById(Integer id) {
        return id == null || mockExamRepository == null
                ? Optional.empty()
                : mockExamRepository.findById(id);
    }

    @Override
    public Optional<MockExamEntity> findMockExamByIdForUpdate(Integer id) {
        return id == null || mockExamRepository == null
                ? Optional.empty()
                : mockExamRepository.findByIdForUpdate(id);
    }

    @Override
    public List<MockExamQuestionEntity> findExamQuestions(Integer examId) {
        return examId == null || mockExamQuestionRepository == null
                ? List.of()
                : mockExamQuestionRepository.findByMockExamIdOrderByPosition(examId);
    }

    @Override
    public List<MockExamSectionEntity> findExamSections(Integer examId) {
        return examId == null || mockExamSectionRepository == null
                ? List.of()
                : mockExamSectionRepository.findByMockExamIdOrderById(examId);
    }

    @Override
    public Optional<QuestionEntity> findQuestionById(Integer id) {
        return id == null || questionRepository == null
                ? Optional.empty()
                : questionRepository.findById(id);
    }

    @Override
    public Optional<QuestionItemEntity> findQuestionItemById(Integer itemId) {
        return itemId == null || questionItemRepository == null
                ? Optional.empty()
                : questionItemRepository.findById(itemId);
    }

    @Override
    public List<QuestionItemEntity> findQuestionItems(Integer questionId) {
        return questionId == null || questionItemRepository == null
                ? List.of()
                : questionItemRepository.findByQuestionIdOrderByPosition(questionId);
    }

    @Override
    public List<QuestionOptionEntity> findQuestionOptions(Integer itemId) {
        return itemId == null || questionOptionRepository == null
                ? List.of()
                : questionOptionRepository.findByQuestionItemIdOrderByPosition(itemId);
    }

    @Override
    public List<QuestionMediaEntity> findQuestionMedia(Integer questionId) {
        return questionId == null || questionMediaRepository == null
                ? List.of()
                : questionMediaRepository.findByQuestionIdOrderByPosition(questionId);
    }

    @Override
    public ExamViewDTO publishedExam(Integer userId, Integer examId) {
        MockExamEntity exam =
                mockExamRepository
                        .findById(examId)
                        .filter(x -> "PUBLISHED".equals(x.getStatus()) && !x.isDemoOnly())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Đề thi chưa được công bố hoặc không tồn tại"));
        ExamSummaryDTO summary = summary(exam);
        List<QuestionViewDTO> views =
                mockExamQuestionRepository.findByMockExamIdOrderByPosition(examId).stream()
                        .map(
                                link ->
                                        questionRepository
                                                .findById(link.getQuestionId())
                                                .filter(q -> "PUBLISHED".equals(q.getStatus()))
                                                .map(
                                                        q ->
                                                                new QuestionViewDTO(
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
        return new ExamViewDTO(summary, views);
    }

    @Override
    public ExamSummaryDTO checkpoint(String skill, String targetLevel) {
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

    private ExamSummaryDTO summary(MockExamEntity x) {
        return new ExamSummaryDTO(
                x.getId(),
                x.getTitle(),
                x.getExamType(),
                x.getSkill(),
                x.getTargetLevel(),
                x.getDurationMinutes(),
                x.getScoringSchemeId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MockExamEntity> findPublishedExamsBySkill(String skill) {
        return mockExamRepository.findBySkillAndStatusOrderByUpdatedAtDesc(skill, "PUBLISHED");
    }

    @Override
    @Transactional(readOnly = true)
    public List<MockExamEntity> findAllPublishedExams() {
        return mockExamRepository.findByStatusOrderByUpdatedAtDesc("PUBLISHED");
    }
}

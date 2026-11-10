package com.aptisprep.mvc.catalog.service;

import com.aptisprep.mvc.catalog.entity.*;
import com.aptisprep.service.contract.IntegrationModels.ExamSummary;
import com.aptisprep.service.contract.IntegrationModels.ExamView;
import java.util.List;
import java.util.Optional;

/** Owner 3. Only published, valid exams; never expose marking keys to templates. */
public interface ExamCatalogService {
    ExamView publishedExam(Integer userId, Integer examId);

    ExamSummary checkpoint(String skill, String targetLevel);

    Optional<MockExamEntity> findMockExamById(Integer id);

    Optional<MockExamEntity> findMockExamByIdForUpdate(Integer id);

    List<MockExamQuestionEntity> findExamQuestions(Integer examId);

    List<MockExamSectionEntity> findExamSections(Integer examId);

    Optional<QuestionEntity> findQuestionById(Integer id);

    Optional<QuestionItemEntity> findQuestionItemById(Integer itemId);

    List<QuestionItemEntity> findQuestionItems(Integer questionId);

    List<QuestionOptionEntity> findQuestionOptions(Integer itemId);

    List<QuestionMediaEntity> findQuestionMedia(Integer questionId);
}

package com.aptisprep.service.admin;

import com.aptisprep.entity.*;
import com.aptisprep.presentation.ExamSummaryDTO;
import com.aptisprep.presentation.ExamViewDTO;
import java.util.List;
import java.util.Optional;

/** Owner 3. Only published, valid exams; never expose marking keys to templates. */
public interface ExamCatalogService {
    ExamViewDTO publishedExam(Integer userId, Integer examId);

    ExamSummaryDTO checkpoint(String skill, String targetLevel);

    Optional<MockExamEntity> findMockExamById(Integer id);

    Optional<MockExamEntity> findMockExamByIdForUpdate(Integer id);

    List<MockExamQuestionEntity> findExamQuestions(Integer examId);

    List<MockExamSectionEntity> findExamSections(Integer examId);

    Optional<QuestionEntity> findQuestionById(Integer id);

    Optional<QuestionItemEntity> findQuestionItemById(Integer itemId);

    List<QuestionItemEntity> findQuestionItems(Integer questionId);

    List<QuestionOptionEntity> findQuestionOptions(Integer itemId);

    List<QuestionMediaEntity> findQuestionMedia(Integer questionId);

    List<MockExamEntity> findPublishedExamsBySkill(String skill);

    List<MockExamEntity> findAllPublishedExams();
}

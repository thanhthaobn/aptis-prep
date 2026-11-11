package com.aptisprep.service.admin;

import com.aptisprep.dto.ExamSummaryDTO;
import com.aptisprep.dto.ExamViewDTO;
import com.aptisprep.entity.*;
import java.util.List;
import java.util.Optional;

/** Owner 3. Only published, valid exams; never expose marking keys to templates. */
public interface ExamCatalogService {
    public ExamViewDTO publishedExam(Integer userId, Integer examId);

    public ExamSummaryDTO checkpoint(String skill, String targetLevel);

    public Optional<MockExamEntity> findMockExamById(Integer id);

    public Optional<MockExamEntity> findMockExamByIdForUpdate(Integer id);

    public List<MockExamQuestionEntity> findExamQuestions(Integer examId);

    public List<MockExamSectionEntity> findExamSections(Integer examId);

    public Optional<QuestionEntity> findQuestionById(Integer id);

    public Optional<QuestionItemEntity> findQuestionItemById(Integer itemId);

    public List<QuestionItemEntity> findQuestionItems(Integer questionId);

    public List<QuestionOptionEntity> findQuestionOptions(Integer itemId);

    public List<QuestionMediaEntity> findQuestionMedia(Integer questionId);

    public List<MockExamEntity> findPublishedExamsBySkill(String skill);

    public List<MockExamEntity> findAllPublishedExams();
}

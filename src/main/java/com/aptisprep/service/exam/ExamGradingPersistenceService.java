package com.aptisprep.service.exam;

import com.aptisprep.entity.ExamAttemptEntity;
import com.aptisprep.entity.ExamAttemptSnapshotEntity;
import com.aptisprep.entity.UserAnswerDetailEntity;
import com.aptisprep.entity.UserAnswerEntity;
import java.util.List;
import java.util.Optional;

public interface ExamGradingPersistenceService {
    public Optional<ExamAttemptEntity> findAttemptById(Integer attemptId);

    public Optional<ExamAttemptEntity> findAttemptByIdAndUserId(Integer attemptId, Integer userId);

    public Optional<ExamAttemptEntity> findAttemptByIdAndUserIdForUpdate(
            Integer attemptId, Integer userId);

    public Optional<ExamAttemptEntity> findAttemptByIdForUpdate(Integer attemptId);

    public ExamAttemptEntity saveAttempt(ExamAttemptEntity attempt);

    public Optional<ExamAttemptSnapshotEntity> findSnapshotByAttemptId(Integer attemptId);

    public List<UserAnswerEntity> findAnswersByAttemptId(Integer attemptId);

    public Optional<UserAnswerEntity> findAnswerByIdAndAttemptId(
            Integer answerId, Integer attemptId);

    public Optional<UserAnswerEntity> findAnswerByAttemptIdAndQuestionId(
            Integer attemptId, Integer questionId);

    public UserAnswerEntity saveAnswer(UserAnswerEntity answer);

    public Optional<UserAnswerDetailEntity> findAnswerDetail(
            Integer userAnswerId, Integer questionItemId);
}

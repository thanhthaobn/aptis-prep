package com.aptisprep.service.exam;

import com.aptisprep.entity.ExamAttemptEntity;
import com.aptisprep.entity.ExamAttemptSnapshotEntity;
import com.aptisprep.entity.UserAnswerDetailEntity;
import com.aptisprep.entity.UserAnswerEntity;
import java.util.List;
import java.util.Optional;

public interface ExamGradingPersistenceService {
    Optional<ExamAttemptEntity> findAttemptById(Integer attemptId);

    Optional<ExamAttemptEntity> findAttemptByIdAndUserId(Integer attemptId, Integer userId);

    Optional<ExamAttemptEntity> findAttemptByIdAndUserIdForUpdate(
            Integer attemptId, Integer userId);

    Optional<ExamAttemptEntity> findAttemptByIdForUpdate(Integer attemptId);

    ExamAttemptEntity saveAttempt(ExamAttemptEntity attempt);

    Optional<ExamAttemptSnapshotEntity> findSnapshotByAttemptId(Integer attemptId);

    List<UserAnswerEntity> findAnswersByAttemptId(Integer attemptId);

    Optional<UserAnswerEntity> findAnswerByIdAndAttemptId(Integer answerId, Integer attemptId);

    Optional<UserAnswerEntity> findAnswerByAttemptIdAndQuestionId(
            Integer attemptId, Integer questionId);

    UserAnswerEntity saveAnswer(UserAnswerEntity answer);

    Optional<UserAnswerDetailEntity> findAnswerDetail(Integer userAnswerId, Integer questionItemId);
}

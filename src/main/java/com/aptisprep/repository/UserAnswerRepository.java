package com.aptisprep.repository;

import com.aptisprep.entity.UserAnswerEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAnswerRepository extends JpaRepository<UserAnswerEntity, Integer> {
    List<UserAnswerEntity> findByAttemptIdOrderByQuestionIdAsc(Integer attemptId);
    Optional<UserAnswerEntity> findByAttemptIdAndQuestionId(Integer attemptId, Integer questionId);
    Optional<UserAnswerEntity> findByIdAndAttemptId(Integer id, Integer attemptId);
    boolean existsByQuestionId(Integer questionId);
}

package com.aptisprep.mvc.repository;

import com.aptisprep.mvc.entity.UserAnswer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAnswerRepository extends JpaRepository<UserAnswer, Integer> {
    List<UserAnswer> findByAttemptIdOrderByQuestionIdAsc(Integer attemptId);

    Optional<UserAnswer> findByAttemptIdAndQuestionId(Integer attemptId, Integer questionId);

    Optional<UserAnswer> findByIdAndAttemptId(Integer id, Integer attemptId);

    boolean existsByQuestionId(Integer questionId);
}








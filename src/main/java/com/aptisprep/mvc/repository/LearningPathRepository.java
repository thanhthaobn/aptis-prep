package com.aptisprep.mvc.repository;

import com.aptisprep.mvc.entity.LearningPath;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningPathRepository extends JpaRepository<LearningPath, Integer> {
    boolean existsByUserIdAndSourceAttemptIdAndStatus(
            Integer userId, Integer attemptId, String status);

    List<LearningPath> findByUserIdAndStatusOrderBySkillAscCreatedAtDesc(
            Integer userId, String status);

    Optional<LearningPath> findFirstByUserIdAndSkillAndStatusOrderByCreatedAtDesc(
            Integer userId, String skill, String status);

    Optional<LearningPath> findByIdAndUserId(Integer id, Integer userId);

    Optional<LearningPath> findByUserIdAndSourceAttemptId(Integer userId, Integer attemptId);
}








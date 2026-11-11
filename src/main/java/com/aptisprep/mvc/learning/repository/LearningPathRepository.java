package com.aptisprep.mvc.learning.repository;

import com.aptisprep.mvc.learning.entity.LearningPath;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LearningPathRepository extends JpaRepository<LearningPath, Integer> {
    boolean existsByUserIdAndSourceAttemptIdAndStatus(
            Integer userId, Integer attemptId, String status);

    @Query(
            "select p from LearningPath p where p.user.id = :userId and p.status = :status order by"
                    + " p.skill asc, p.createdAt desc")
    List<LearningPath> findByUserIdAndStatusOrderBySkillAscCreatedAtDesc(
            Integer userId, String status);

    @Query(
            "select p from LearningPath p where p.user.id = :userId and p.skill = :skill and"
                    + " p.status = :status order by p.createdAt desc")
    Optional<LearningPath> findFirstByUserIdAndSkillAndStatusOrderByCreatedAtDesc(
            Integer userId, String skill, String status);

    @Query("select p from LearningPath p where p.id = :id and p.user.id = :userId")
    Optional<LearningPath> findByIdAndUserId(Integer id, Integer userId);

    @Query(
            "select p from LearningPath p where p.user.id = :userId and p.sourceAttemptId ="
                    + " :attemptId")
    Optional<LearningPath> findByUserIdAndSourceAttemptId(Integer userId, Integer attemptId);
}

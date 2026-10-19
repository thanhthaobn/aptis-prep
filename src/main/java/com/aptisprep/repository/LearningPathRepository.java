package com.aptisprep.repository;

import com.aptisprep.entity.LearningPathEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningPathRepository extends JpaRepository<LearningPathEntity, Integer> {
    List<LearningPathEntity> findByUserIdAndStatusOrderBySkillAscCreatedAtDesc(Integer userId, String status);
    Optional<LearningPathEntity> findFirstByUserIdAndSkillAndStatusOrderByCreatedAtDesc(Integer userId, String skill, String status);
    Optional<LearningPathEntity> findByIdAndUserId(Integer id, Integer userId);
}

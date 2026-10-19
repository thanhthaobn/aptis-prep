package com.aptisprep.repository;

import com.aptisprep.entity.LearningPathItemEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningPathItemRepository extends JpaRepository<LearningPathItemEntity, Integer> {
    List<LearningPathItemEntity> findByPathIdOrderByPositionAsc(Integer pathId);
    Optional<LearningPathItemEntity> findByPathIdAndLessonId(Integer pathId, Integer lessonId);
}

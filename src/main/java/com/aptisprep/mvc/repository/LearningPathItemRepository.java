package com.aptisprep.mvc.repository;

import com.aptisprep.mvc.entity.LearningPathItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningPathItemRepository extends JpaRepository<LearningPathItem, Integer> {
    List<LearningPathItem> findByLearningPathUserIdAndLearningPathStatusAndLessonId(
            Integer userId, String status, Integer lessonId);

    List<LearningPathItem> findByLearningPathIdOrderByPositionAsc(Integer pathId);

    Optional<LearningPathItem> findByLearningPathIdAndLessonId(Integer pathId, Integer lessonId);
}

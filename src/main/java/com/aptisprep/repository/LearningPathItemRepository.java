package com.aptisprep.repository;

import com.aptisprep.entity.LearningPathItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningPathItemRepository extends JpaRepository<LearningPathItem, Integer> {
    @org.springframework.data.jpa.repository.Query("select i from LearningPathItem i where i.learningPath.id = :pathId order by i.position asc")
    List<LearningPathItem> findByPathIdOrderByPositionAsc(Integer pathId);
    @org.springframework.data.jpa.repository.Query("select i from LearningPathItem i where i.learningPath.id = :pathId and i.lesson.id = :lessonId")
    Optional<LearningPathItem> findByPathIdAndLessonId(Integer pathId, Integer lessonId);
    @org.springframework.data.jpa.repository.Query("select i from LearningPathItem i where i.learningPath.id = :pathId order by i.position asc")
    List<LearningPathItem> findByLearningPathIdOrderByPositionAsc(Integer pathId);
}

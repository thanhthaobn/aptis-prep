package com.aptisprep.mvc.learning.repository;

import com.aptisprep.mvc.learning.entity.LearningPathItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LearningPathItemRepository extends JpaRepository<LearningPathItem, Integer> {
    @Query(
            "select i from LearningPathItem i where i.learningPath.user.id = :userId and"
                    + " i.learningPath.status = :status and i.lesson.id = :lessonId")
    List<LearningPathItem> findByLearningPathUserIdAndLearningPathStatusAndLessonId(
            Integer userId, String status, Integer lessonId);

    @Query(
            "select i from LearningPathItem i where i.learningPath.id = :pathId order by i.position"
                    + " asc")
    List<LearningPathItem> findByPathIdOrderByPositionAsc(Integer pathId);

    @Query(
            "select i from LearningPathItem i where i.learningPath.id = :pathId and i.lesson.id ="
                    + " :lessonId")
    Optional<LearningPathItem> findByPathIdAndLessonId(Integer pathId, Integer lessonId);

    @Query(
            "select i from LearningPathItem i where i.learningPath.id = :pathId order by i.position"
                    + " asc")
    List<LearningPathItem> findByLearningPathIdOrderByPositionAsc(Integer pathId);
}

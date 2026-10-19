package com.aptisprep.repository;
import com.aptisprep.entity.LearningPathItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface LearningPathItemRepository extends JpaRepository<LearningPathItem,Integer>{
    List<LearningPathItem> findByLearningPathIdOrderByPositionAsc(Integer pathId);
    List<LearningPathItem> findByLearningPathUserIdAndLearningPathStatusAndLessonId(Integer userId,String pathStatus,Integer lessonId);
}

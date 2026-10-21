package com.aptisprep.repository;
import com.aptisprep.entity.LessonProgress;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Integer> {
    Optional<LessonProgress> findByUserIdAndLessonId(Integer userId, Integer lessonId);
    List<LessonProgress> findByUserId(Integer userId);
    long countByLessonId(Integer lessonId);
    long countByLessonIdAndStatus(Integer lessonId,String status);
}

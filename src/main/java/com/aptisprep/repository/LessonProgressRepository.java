package com.aptisprep.repository;
import com.aptisprep.entity.LessonProgress;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Integer> {
    Optional<LessonProgress> findByUserIdAndLessonId(Integer userId, Integer lessonId);
    long countByLessonId(Integer lessonId);
    long countByLessonIdAndStatus(Integer lessonId,String status);
}

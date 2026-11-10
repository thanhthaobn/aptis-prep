package com.aptisprep.mvc.learning.repository;

import com.aptisprep.mvc.learning.entity.LessonCheckAttempt;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonCheckAttemptRepository extends JpaRepository<LessonCheckAttempt, Integer> {
    List<LessonCheckAttempt> findByUserIdAndLessonIdOrderBySubmittedAtDesc(
            Integer userId, Integer lessonId);

    List<LessonCheckAttempt> findByUserId(Integer userId);

    boolean existsByUserIdAndLessonId(Integer userId, Integer lessonId);

    List<LessonCheckAttempt> findByLessonId(Integer lessonId);
}

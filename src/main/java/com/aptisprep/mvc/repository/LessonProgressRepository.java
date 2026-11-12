package com.aptisprep.mvc.repository;

import com.aptisprep.mvc.entity.LessonProgress;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Integer> {
    List<LessonProgress> findByUserId(Integer userId);

    long countByLessonId(Integer lessonId);

    long countByLessonIdAndStatus(Integer lessonId, String status);

    Optional<LessonProgress> findByUserIdAndLessonId(Integer userId, Integer lessonId);

    List<LessonProgress> findByUserIdAndLessonIdIn(Integer userId, Collection<Integer> lessonIds);
}








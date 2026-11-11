package com.aptisprep.mvc.learning.repository;

import com.aptisprep.mvc.learning.entity.LessonCheck;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonCheckRepository extends JpaRepository<LessonCheck, Integer> {
    List<LessonCheck> findByLessonIdOrderByPositionAsc(Integer lessonId);

    void deleteByLessonId(Integer lessonId);

    long countByLessonId(Integer lessonId);
}

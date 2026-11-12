package com.aptisprep.mvc.repository;

import com.aptisprep.mvc.entity.LessonCheck;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonCheckRepository extends JpaRepository<LessonCheck, Integer> {
    List<LessonCheck> findByLessonIdOrderByPositionAsc(Integer lessonId);

    void deleteByLessonId(Integer lessonId);

    long countByLessonId(Integer lessonId);
}








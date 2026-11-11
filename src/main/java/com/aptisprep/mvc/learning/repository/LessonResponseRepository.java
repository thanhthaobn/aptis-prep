package com.aptisprep.mvc.learning.repository;

import com.aptisprep.mvc.learning.entity.LessonResponse;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonResponseRepository extends JpaRepository<LessonResponse, Integer> {
    Optional<LessonResponse> findByUserIdAndLessonId(Integer userId, Integer lessonId);
}

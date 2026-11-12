package com.aptisprep.mvc.repository;

import com.aptisprep.mvc.entity.LessonResponse;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonResponseRepository extends JpaRepository<LessonResponse, Integer> {
    Optional<LessonResponse> findByUserIdAndLessonId(Integer userId, Integer lessonId);
}








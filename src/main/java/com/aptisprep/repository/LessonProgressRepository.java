package com.aptisprep.repository;

import com.aptisprep.entity.LessonProgressEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonProgressRepository extends JpaRepository<LessonProgressEntity, Integer> {
    Optional<LessonProgressEntity> findByUserIdAndLessonId(Integer userId, Integer lessonId);
    List<LessonProgressEntity> findByUserIdAndLessonIdIn(Integer userId, Collection<Integer> lessonIds);
}

package com.aptisprep.repository;

import com.aptisprep.entity.LessonProgress;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Integer> {
    List<LessonProgress> findByUserId(Integer userId);
    @org.springframework.data.jpa.repository.Query("select count(p) from LessonProgress p where p.lesson.id = :lessonId")
    long countByLessonId(@org.springframework.data.repository.query.Param("lessonId") Integer lessonId);
    @org.springframework.data.jpa.repository.Query("select count(p) from LessonProgress p where p.lesson.id = :lessonId and p.status = :status")
    long countByLessonIdAndStatus(@org.springframework.data.repository.query.Param("lessonId") Integer lessonId, @org.springframework.data.repository.query.Param("status") String status);
    @org.springframework.data.jpa.repository.Query("select p from LessonProgress p where p.user.id = :userId and p.lesson.id = :lessonId")
    Optional<LessonProgress> findByUserIdAndLessonId(@org.springframework.data.repository.query.Param("userId") Integer userId, @org.springframework.data.repository.query.Param("lessonId") Integer lessonId);
    @org.springframework.data.jpa.repository.Query("select p from LessonProgress p where p.user.id = :userId and p.lesson.id in :lessonIds")
    List<LessonProgress> findByUserIdAndLessonIdIn(@org.springframework.data.repository.query.Param("userId") Integer userId, @org.springframework.data.repository.query.Param("lessonIds") Collection<Integer> lessonIds);
}

package com.aptisprep.repository;

import com.aptisprep.entity.LessonProgress;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Integer> {
    List<LessonProgress> findByUserId(Integer userId);

    @Query("select count(p) from LessonProgress p where p.lesson.id = :lessonId")
    long countByLessonId(@Param("lessonId") Integer lessonId);

    @Query(
            "select count(p) from LessonProgress p where p.lesson.id = :lessonId and p.status ="
                    + " :status")
    long countByLessonIdAndStatus(
            @Param("lessonId") Integer lessonId, @Param("status") String status);

    @Query("select p from LessonProgress p where p.user.id = :userId and p.lesson.id = :lessonId")
    Optional<LessonProgress> findByUserIdAndLessonId(
            @Param("userId") Integer userId, @Param("lessonId") Integer lessonId);

    @Query("select p from LessonProgress p where p.user.id = :userId and p.lesson.id in :lessonIds")
    List<LessonProgress> findByUserIdAndLessonIdIn(
            @Param("userId") Integer userId, @Param("lessonIds") Collection<Integer> lessonIds);
}

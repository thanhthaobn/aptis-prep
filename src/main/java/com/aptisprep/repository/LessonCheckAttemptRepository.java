package com.aptisprep.repository;
import com.aptisprep.entity.LessonCheckAttempt;import java.util.List;import org.springframework.data.jpa.repository.JpaRepository;
public interface LessonCheckAttemptRepository extends JpaRepository<LessonCheckAttempt,Integer>{List<LessonCheckAttempt> findByUserIdAndLessonIdOrderBySubmittedAtDesc(Integer userId,Integer lessonId);boolean existsByUserIdAndLessonId(Integer userId,Integer lessonId);List<LessonCheckAttempt> findByLessonId(Integer lessonId);}

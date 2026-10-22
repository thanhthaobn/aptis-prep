package com.aptisprep.repository;

import com.aptisprep.entity.LessonEngagement;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonEngagementRepository extends JpaRepository<LessonEngagement, Integer> {
    Optional<LessonEngagement> findByUserIdAndLessonId(Integer userId, Integer lessonId);

    long countByLessonIdAndAudioPlayedAtIsNotNull(Integer lessonId);
}

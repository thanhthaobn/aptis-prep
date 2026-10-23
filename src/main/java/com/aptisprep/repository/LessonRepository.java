package com.aptisprep.repository;

import com.aptisprep.entity.Lesson;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, Integer> {
    List<Lesson> findBySkillAndStatusOrderByDisplayOrderAsc(String skill, String status);

    List<Lesson> findAllByOrderBySkillAscCefrLevelAscDisplayOrderAsc();

    List<Lesson> findByStatusOrderBySkillAscCefrLevelAscDisplayOrderAsc(String status);

    boolean existsBySkillAndCefrLevelAndDisplayOrderAndIdNot(String skill, String cefrLevel, Integer displayOrder, Integer id);
}

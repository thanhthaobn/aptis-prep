package com.aptisprep.repository;

import com.aptisprep.entity.LessonEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<LessonEntity, Integer> {
    List<LessonEntity> findBySkillAndStatusOrderByDisplayOrderAsc(String skill, String status);
}

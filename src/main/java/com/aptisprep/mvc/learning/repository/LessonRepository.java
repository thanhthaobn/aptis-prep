package com.aptisprep.mvc.learning.repository;

import com.aptisprep.mvc.learning.entity.Lesson;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, Integer> {
    List<Lesson> findBySkillAndStatusOrderByDisplayOrderAsc(String skill, String status);

    List<Lesson> findAllByOrderBySkillAscCefrLevelAscDisplayOrderAsc();

    List<Lesson> findByStatusOrderBySkillAscCefrLevelAscDisplayOrderAsc(String status);

    boolean existsBySkillAndCefrLevelAndDisplayOrderAndIdNot(
            String skill, String cefrLevel, Integer displayOrder, Integer id);

    Optional<Lesson> findFirstByAudioPath(String audioPath);
}

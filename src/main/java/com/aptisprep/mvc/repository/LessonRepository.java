package com.aptisprep.mvc.repository;

import com.aptisprep.mvc.entity.Lesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonRepository extends JpaRepository<Lesson, Integer> {
    List<Lesson> findBySkillAndStatusOrderByDisplayOrderAsc(String skill, String status);

    List<Lesson> findAllByOrderBySkillAscCefrLevelAscDisplayOrderAsc();

    @Query(
            "select l from Lesson l "
                    + "where (:pattern = '%' or "
                    + "lower(coalesce(l.title, '')) like :pattern or "
                    + "lower(coalesce(l.description, '')) like :pattern or "
                    + "lower(coalesce(l.topic, '')) like :pattern) "
                    + "and (:skill = '' or l.skill = :skill) "
                    + "and (:level = '' or l.cefrLevel = :level) "
                    + "order by l.skill asc, l.cefrLevel asc, l.displayOrder asc, l.id asc")
    Page<Lesson> searchAdminLessons(
            @Param("pattern") String pattern,
            @Param("skill") String skill,
            @Param("level") String level,
            Pageable pageable);

    List<Lesson> findByStatusOrderBySkillAscCefrLevelAscDisplayOrderAsc(String status);

    boolean existsBySkillAndCefrLevelAndDisplayOrderAndIdNot(
            String skill, String cefrLevel, Integer displayOrder, Integer id);

    Optional<Lesson> findFirstByAudioPath(String audioPath);
}








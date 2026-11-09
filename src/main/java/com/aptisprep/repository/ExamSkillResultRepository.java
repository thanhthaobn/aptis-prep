package com.aptisprep.repository;

import com.aptisprep.entity.ExamSkillResultEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamSkillResultRepository extends JpaRepository<ExamSkillResultEntity, Integer> {
    boolean existsByAttemptId(Integer attemptId);

    List<ExamSkillResultEntity> findByAttemptIdOrderBySkillAsc(Integer attemptId);

    Optional<ExamSkillResultEntity> findByAttemptIdAndSkill(Integer attemptId, String skill);
}

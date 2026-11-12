package com.aptisprep.mvc.repository;

import com.aptisprep.mvc.entity.ExamSkillResult;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamSkillResultRepository extends JpaRepository<ExamSkillResult, Integer> {
    boolean existsByAttemptId(Integer attemptId);

    List<ExamSkillResult> findByAttemptIdOrderBySkillAsc(Integer attemptId);

    Optional<ExamSkillResult> findByAttemptIdAndSkill(Integer attemptId, String skill);
}








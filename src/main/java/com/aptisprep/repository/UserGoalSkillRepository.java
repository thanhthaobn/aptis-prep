package com.aptisprep.repository;

import com.aptisprep.entity.UserGoalSkill;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGoalSkillRepository extends JpaRepository<UserGoalSkill, Integer> {
    List<UserGoalSkill> findByGoalId(Integer goalId);
}

package com.aptisprep.mvc.account.repository;

import com.aptisprep.mvc.account.entity.UserGoalSkill;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGoalSkillRepository extends JpaRepository<UserGoalSkill, Integer> {
    List<UserGoalSkill> findByGoalId(Integer goalId);
}

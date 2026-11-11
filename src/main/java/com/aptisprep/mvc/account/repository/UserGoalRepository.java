package com.aptisprep.mvc.account.repository;

import com.aptisprep.mvc.account.entity.UserGoal;
import com.aptisprep.mvc.common.enums.GoalStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGoalRepository extends JpaRepository<UserGoal, Integer> {
    Optional<UserGoal> findFirstByUserIdAndStatusOrderByCreatedAtDesc(
            Integer userId, GoalStatus status);
}

package com.aptisprep.mvc.repository;

import com.aptisprep.mvc.entity.UserGoal;
import com.aptisprep.mvc.enums.GoalStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGoalRepository extends JpaRepository<UserGoal, Integer> {
    Optional<UserGoal> findFirstByUserIdAndStatusOrderByCreatedAtDesc(
            Integer userId, GoalStatus status);
}

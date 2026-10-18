package com.aptisprep.repository;

import com.aptisprep.entity.ProfileApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileApplicationRepository extends JpaRepository<ProfileApplication, Integer> {
    boolean existsByUserIdAndAttemptIdAndSkill(Integer userId, Integer attemptId, String skill);
}

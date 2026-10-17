package com.aptisprep.repository;

import com.aptisprep.entity.UserSkillProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSkillProfileRepository extends JpaRepository<UserSkillProfile, Integer> {
    List<UserSkillProfile> findByUserIdOrderBySkillAsc(Integer userId);
    Optional<UserSkillProfile> findByUserIdAndSkill(Integer userId, String skill);
}

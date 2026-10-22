package com.aptisprep.repository;

import com.aptisprep.entity.ScoringScaleRule;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoringScaleRuleRepository extends JpaRepository<ScoringScaleRule, Integer> {
    Optional<ScoringScaleRule> findBySchemeIdAndSkillIgnoreCase(Integer schemeId, String skill);
}

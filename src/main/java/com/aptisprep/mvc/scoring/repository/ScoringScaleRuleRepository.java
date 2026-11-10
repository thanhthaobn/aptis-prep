package com.aptisprep.mvc.scoring.repository;

import com.aptisprep.mvc.scoring.entity.ScoringScaleRule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoringScaleRuleRepository extends JpaRepository<ScoringScaleRule, Integer> {
    List<ScoringScaleRule> findBySchemeId(Integer schemeId);

    Optional<ScoringScaleRule> findBySchemeIdAndSkillIgnoreCase(Integer schemeId, String skill);
}

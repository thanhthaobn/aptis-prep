package com.aptisprep.mvc.repository;

import com.aptisprep.mvc.entity.ScoreBand;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoreBandRepository extends JpaRepository<ScoreBand, Integer> {
    List<ScoreBand> findBySchemeIdAndSkillOrderByMinScoreAsc(Integer schemeId, String skill);

    List<ScoreBand> findBySchemeIdOrderBySkillAscMinScoreAsc(Integer schemeId);

    Optional<ScoreBand> findByIdAndSchemeId(Integer id, Integer schemeId);

    boolean existsBySchemeIdAndSkillAndCefrLevelIgnoreCase(
            Integer schemeId, String skill, String cefrLevel);

    boolean existsBySchemeIdAndSkillAndCefrLevelIgnoreCaseAndIdNot(
            Integer schemeId, String skill, String cefrLevel, Integer id);

    List<ScoreBand> findBySchemeIdAndSkillAndMaxScoreGreaterThanEqualAndMinScoreLessThanEqual(
            Integer schemeId, String skill, BigDecimal min, BigDecimal max);
}

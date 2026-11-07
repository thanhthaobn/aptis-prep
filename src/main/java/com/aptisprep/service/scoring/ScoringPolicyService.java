package com.aptisprep.service.scoring;

import com.aptisprep.entity.ScoreBand;
import com.aptisprep.entity.ScoringScaleRule;
import com.aptisprep.entity.ScoringScheme;
import com.aptisprep.enums.CefrLevel;
import com.aptisprep.enums.RecordStatus;
import com.aptisprep.enums.Skill;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.ScoreBandRepository;
import com.aptisprep.repository.ScoringScaleRuleRepository;
import com.aptisprep.repository.ScoringSchemeRepository;
import com.aptisprep.service.contract.ScoringPolicyContract;
import com.aptisprep.service.contract.ScoringPolicyContract.SkillScore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Estimated internal policy. Raw and scale ranges come from the skill/version rule; no official
 * Aptis claim.
 */
@Service
public class ScoringPolicyService implements ScoringPolicyContract {
    private final ScoringSchemeRepository scoringSchemeRepository;
    private final ScoreBandRepository scoreBandRepository;
    private final ScoringScaleRuleRepository scoringScaleRuleRepository;

    public ScoringPolicyService(
            ScoringSchemeRepository scoringSchemeRepository,
            ScoreBandRepository scoreBandRepository,
            ScoringScaleRuleRepository scoringScaleRuleRepository) {
        this.scoringSchemeRepository = scoringSchemeRepository;
        this.scoreBandRepository = scoreBandRepository;
        this.scoringScaleRuleRepository = scoringScaleRuleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal raw(Integer schemeId, String skill, int correct, int total) {
        String normalized = normalizeSkill(skill);
        if (total <= 0 || correct < 0 || correct > total) {
            throw new BusinessRuleException("Số câu đúng/tổng số câu không hợp lệ");
        }
        ScoringScaleRule rule = requireRule(schemeId, normalized);
        return rule.getRawMin()
                .add(
                        rule.getRawMax()
                                .subtract(rule.getRawMin())
                                .multiply(BigDecimal.valueOf(correct))
                                .divide(BigDecimal.valueOf(total), 8, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal scale(Integer schemeId, String skill, BigDecimal rawScore) {
        String normalized = normalizeSkill(skill);
        if (rawScore == null) return null;
        ScoringScaleRule rule = requireRule(schemeId, normalized);
        if (rawScore.compareTo(rule.getRawMin()) < 0 || rawScore.compareTo(rule.getRawMax()) > 0) {
            throw new BusinessRuleException("Điểm raw nằm ngoài policy của kỹ năng");
        }
        BigDecimal rawRange = rule.getRawMax().subtract(rule.getRawMin());
        if (rawRange.signum() <= 0) throw new BusinessRuleException("Policy raw không hợp lệ");
        return rule.getScaleMin()
                .add(
                        rule.getScaleMax()
                                .subtract(rule.getScaleMin())
                                .multiply(rawScore.subtract(rule.getRawMin()))
                                .divide(rawRange, 8, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public String estimatedLevel(Integer schemeId, String skill, BigDecimal scaleScore) {
        String normalized = normalizeSkill(skill);
        if (scaleScore == null) return null;
        ScoringScaleRule rule = requireRule(schemeId, normalized);
        if (scaleScore.compareTo(rule.getScaleMin()) < 0
                || scaleScore.compareTo(rule.getScaleMax()) > 0) {
            throw new BusinessRuleException("Điểm scale nằm ngoài policy của kỹ năng");
        }
        return scoreBandRepository
                .findBySchemeIdAndSkillOrderByMinScoreAsc(schemeId, normalized)
                .stream()
                .filter(
                        band ->
                                scaleScore.compareTo(band.getMinScore()) >= 0
                                        && scaleScore.compareTo(band.getMaxScore()) <= 0)
                .map(ScoreBand::getCefrLevel)
                .findFirst()
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public String policyVersion(Integer schemeId) {
        return requireActiveScheme(schemeId).getVersion();
    }

    @Override
    @Transactional(readOnly = true, noRollbackFor = BusinessRuleException.class)
    public void validatePolicy(Integer schemeId, String skill) {
        requireActiveScheme(schemeId);
        String normalized = normalizeSkill(skill);
        ScoringScaleRule rule = requireRule(schemeId, normalized);
        ScoreBandCoverage.requireComplete(
                rule,
                scoreBandRepository.findBySchemeIdAndSkillOrderByMinScoreAsc(schemeId, normalized));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal overallScale(Integer schemeId, List<SkillScore> skillScores) {
        requireHistoricalScheme(schemeId);
        if (skillScores == null || skillScores.isEmpty()) return null;
        return skillScores.stream()
                .map(SkillScore::scaleScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(skillScores.size()), 2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public String overallLevel(Integer schemeId, List<SkillScore> skillScores) {
        requireHistoricalScheme(schemeId);
        if (skillScores == null || skillScores.isEmpty()) return null;
        return skillScores.stream()
                .map(SkillScore::cefrLevel)
                .filter(Objects::nonNull)
                .min(Comparator.comparingInt(this::levelOrder))
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public FullMockScore fullMockScore(Integer schemeId, List<SkillScore> skillScores) {
        requireHistoricalScheme(schemeId);
        if (skillScores.size() != Skill.values().length
                || !skillScores.stream()
                        .map(SkillScore::skill)
                        .collect(Collectors.toSet())
                        .containsAll(Skill.codes())
                || skillScores.stream()
                        .anyMatch(
                                score ->
                                        score.scaleScore() == null
                                                || CefrLevel.parse(score.cefrLevel()) == null)) {
            return new FullMockScore(null, null, skillScores);
        }
        CefrLevel coreLevel =
                skillScores.stream()
                        .filter(score -> Skill.GRAMMAR_VOCABULARY.name().equals(score.skill()))
                        .map(score -> CefrLevel.parse(score.cefrLevel()))
                        .findFirst()
                        .orElseThrow();
        List<SkillScore> adjusted = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int levels = 0;
        for (SkillScore score : skillScores) {
            if (Skill.GRAMMAR_VOCABULARY.name().equals(score.skill())) {
                adjusted.add(new SkillScore(score.skill(), score.scaleScore(), null));
                continue;
            }
            ScoringScaleRule rule = requireRule(schemeId, score.skill());
            BigDecimal range = rule.getScaleMax().subtract(rule.getScaleMin());
            total =
                    total.add(
                            score.scaleScore()
                                    .subtract(rule.getScaleMin())
                                    .multiply(BigDecimal.valueOf(50))
                                    .divide(range, 8, RoundingMode.HALF_UP));
            CefrLevel level = borderlineLevel(schemeId, score, coreLevel, range);
            levels += level.ordinal();
            adjusted.add(new SkillScore(score.skill(), score.scaleScore(), level.name()));
        }
        // Aptis Scoring System v2.1, section 8: round the mean of the four CEFR levels.
        // The published examples round a midpoint up to the higher level.
        int average = Math.round(levels / 4.0f);
        return new FullMockScore(
                total.setScale(2, RoundingMode.HALF_UP),
                CefrLevel.values()[average].name(),
                List.copyOf(adjusted));
    }

    private CefrLevel borderlineLevel(
            Integer schemeId, SkillScore score, CefrLevel coreLevel, BigDecimal range) {
        CefrLevel level = CefrLevel.parse(score.cefrLevel());
        // The local policy uses a one-point window on a 0–50 equivalent scale.
        for (ScoreBand band :
                scoreBandRepository.findBySchemeIdAndSkillOrderByMinScoreAsc(
                        schemeId, score.skill())) {
            CefrLevel next = CefrLevel.parse(band.getCefrLevel());
            BigDecimal gap = band.getMinScore().subtract(score.scaleScore());
            if (gap.signum() <= 0) continue;
            if (next != null
                    && next.ordinal() == level.ordinal() + 1
                    && coreLevel.atLeast(next)
                    && gap.multiply(BigDecimal.valueOf(50)).compareTo(range) <= 0) {
                return next;
            }
            break;
        }
        return level;
    }

    private ScoringScheme requireActiveScheme(Integer id) {
        if (id == null) throw new BusinessRuleException("Thiếu phiên bản bộ chấm điểm");
        ScoringScheme scheme =
                scoringSchemeRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy bộ chấm điểm"));
        if (scheme.getStatus() != RecordStatus.ACTIVE
                || scheme.getCertificate().getStatus() != RecordStatus.ACTIVE) {
            throw new BusinessRuleException("Bộ chấm điểm hoặc chứng chỉ đã ngừng sử dụng");
        }
        return scheme;
    }

    /** Historical attempts keep using their immutable scheme even after it is retired. */
    private ScoringScheme requireHistoricalScheme(Integer id) {
        if (id == null) throw new BusinessRuleException("Thiếu phiên bản bộ chấm điểm");
        return scoringSchemeRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ chấm điểm"));
    }

    private ScoringScaleRule requireRule(Integer schemeId, String skill) {
        requireHistoricalScheme(schemeId);
        return scoringScaleRuleRepository
                .findBySchemeIdAndSkillIgnoreCase(schemeId, skill)
                .orElseThrow(
                        () ->
                                new BusinessRuleException(
                                        "Chưa cấu hình raw-to-scale cho kỹ năng và version này"));
    }

    private String normalizeSkill(String skill) {
        if (skill == null) throw new BusinessRuleException("Thiếu kỹ năng");
        String value = skill.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        try {
            Skill.valueOf(value);
            return value;
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException("Kỹ năng không hợp lệ");
        }
    }

    private int levelOrder(String value) {
        return switch (value == null ? "" : value) {
            case "A1" -> 1;
            case "A2" -> 2;
            case "B1" -> 3;
            case "B2" -> 4;
            case "C1" -> 5;
            case "C2" -> 6;
            default -> 0;
        };
    }
}

package com.aptisprep.service.scoring.impl;

import com.aptisprep.service.scoring.ScoringPolicyService;
import com.aptisprep.service.scoring.ScoreBandCoverage;

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
public class ScoringPolicyServiceImpl implements ScoringPolicyService {
    private static final List<String> FULL_MOCK_LEVELS = CefrLevel.scoringCodes();
    private final ScoringSchemeRepository scoringSchemeRepository;
    private final ScoreBandRepository scoreBandRepository;
    private final ScoringScaleRuleRepository scoringScaleRuleRepository;

    public ScoringPolicyServiceImpl(
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
                .map(
                        level ->
                                requireHistoricalScheme(schemeId)
                                        .getCertificate()
                                        .scoringLevel(level))
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean belowConfiguredTarget(Integer schemeId, String skill, BigDecimal scaleScore) {
        var scheme = requireHistoricalScheme(schemeId);
        if (scheme.getScopeSkill() == null || !scheme.getScopeSkill().equals(normalizeSkill(skill)))
            return false;
        var bands =
                scoreBandRepository.findBySchemeIdAndSkillOrderByMinScoreAsc(
                        schemeId, scheme.getScopeSkill());
        return bands.size() == 1 && scaleScore.compareTo(bands.getFirst().getMinScore()) < 0;
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
        var certificate = requireHistoricalScheme(schemeId).getCertificate();
        if (skillScores == null || skillScores.isEmpty()) return null;
        return skillScores.stream()
                .map(SkillScore::cefrLevel)
                .filter(Objects::nonNull)
                .map(certificate::scoringLevel)
                .min(Comparator.comparingInt(this::levelOrder))
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public FullMockScore fullMockScore(Integer schemeId, List<SkillScore> skillScores) {
        var certificate = requireHistoricalScheme(schemeId).getCertificate();
        if (skillScores == null
                || skillScores.size() != Skill.values().length
                || !skillScores.stream()
                        .map(SkillScore::skill)
                        .collect(Collectors.toSet())
                        .containsAll(Skill.codes())
                || skillScores.stream()
                        .anyMatch(
                                score ->
                                        score.scaleScore() == null
                                                || !FULL_MOCK_LEVELS.contains(score.cefrLevel()))) {
            return new FullMockScore(null, null, skillScores == null ? List.of() : skillScores);
        }
        int coreLevel =
                skillScores.stream()
                        .filter(score -> Skill.GRAMMAR_VOCABULARY.name().equals(score.skill()))
                        .mapToInt(score -> FULL_MOCK_LEVELS.indexOf(score.cefrLevel()))
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
            int level = borderlineLevel(schemeId, score, coreLevel, range);
            String reportedLevel = certificate.scoringLevel(FULL_MOCK_LEVELS.get(level));
            level = FULL_MOCK_LEVELS.indexOf(reportedLevel);
            levels += level;
            adjusted.add(new SkillScore(score.skill(), score.scaleScore(), reportedLevel));
        }
        // Aptis Scoring System v2.1, section 8: round the mean of the four CEFR levels.
        // The published examples round a midpoint up to the higher level.
        int average = Math.round(levels / 4.0f);
        return new FullMockScore(
                total.setScale(2, RoundingMode.HALF_UP),
                FULL_MOCK_LEVELS.get(average),
                List.copyOf(adjusted));
    }

    private int borderlineLevel(
            Integer schemeId, SkillScore score, int coreLevel, BigDecimal range) {
        int level = FULL_MOCK_LEVELS.indexOf(score.cefrLevel());
        if (CefrLevel.BELOW_A1.equals(score.cefrLevel())) return level;
        // The local policy uses a one-point window on a 0–50 equivalent scale.
        for (ScoreBand band :
                scoreBandRepository.findBySchemeIdAndSkillOrderByMinScoreAsc(
                        schemeId, score.skill())) {
            int next = FULL_MOCK_LEVELS.indexOf(band.getCefrLevel());
            BigDecimal gap = band.getMinScore().subtract(score.scaleScore());
            if (gap.signum() <= 0) continue;
            if (next == level + 1
                    && coreLevel >= next
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



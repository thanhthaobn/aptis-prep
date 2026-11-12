package com.aptisprep.mvc.service.scoring.impl;

import com.aptisprep.mvc.dto.ExamAttemptDTO;
import com.aptisprep.mvc.dto.ExamSkillResultDTO;
import com.aptisprep.mvc.entity.ScoreBand;
import com.aptisprep.mvc.entity.ScoringScaleRule;
import com.aptisprep.mvc.entity.ScoringScheme;
import com.aptisprep.mvc.enums.CefrLevel;
import com.aptisprep.mvc.enums.RecordStatus;
import com.aptisprep.mvc.enums.Skill;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.exception.ResourceNotFoundException;
import com.aptisprep.mvc.repository.ScoreBandRepository;
import com.aptisprep.mvc.repository.ScoringScaleRuleRepository;
import com.aptisprep.mvc.repository.ScoringSchemeRepository;
import com.aptisprep.mvc.service.scoring.ScoringPolicyService;
import com.aptisprep.mvc.support.scoring.ScoreBandCoverage;
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
 * Policy scoring nội bộ của hệ thống.
 *
 * <p>Service chuyển số câu đúng thành raw score, raw thành scale score và scale thành CEFR band.
 * Rule được tải theo scheme/version, nên attempt cũ vẫn dùng policy ban đầu dù admin kích hoạt
 * scheme mới.
 */
@Service
public class ScoringPolicyServiceImpl implements ScoringPolicyService {
    private List<String> FULL_MOCK_LEVELS = CefrLevel.scoringCodes();
    private ScoringSchemeRepository scoringSchemeRepository;
    private ScoreBandRepository scoreBandRepository;
    private ScoringScaleRuleRepository scoringScaleRuleRepository;

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
    // Chuyển tỷ lệ câu đúng vào raw range được cấu hình cho skill và scheme.
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
    public BigDecimal raw(Integer schemeId, String skill, BigDecimal earnedRaw, BigDecimal rawMax) {
        if (earnedRaw == null
                || rawMax == null
                || rawMax.signum() <= 0
                || earnedRaw.signum() < 0
                || earnedRaw.compareTo(rawMax) > 0) {
            throw new BusinessRuleException("Điểm raw đạt được/tối đa không hợp lệ");
        }
        // Practice mode derives rawMax from the locked exam, not from an admin-entered range.
        requireRule(schemeId, normalizeSkill(skill));
        return earnedRaw.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    // Chuyển raw score vào scale range trước khi tra CEFR band.
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
    public BigDecimal scale(
            Integer schemeId, String skill, BigDecimal rawScore, BigDecimal rawMax) {
        String normalized = normalizeSkill(skill);
        if (rawScore == null
                || rawMax == null
                || rawMax.signum() <= 0
                || rawScore.signum() < 0
                || rawScore.compareTo(rawMax) > 0) {
            throw new BusinessRuleException("Điểm raw theo đề thi không hợp lệ");
        }
        ScoringScaleRule rule = requireRule(schemeId, normalized);
        BigDecimal scaleRange = rule.getScaleMax().subtract(rule.getScaleMin());
        if (scaleRange.signum() <= 0) throw new BusinessRuleException("Policy scale không hợp lệ");
        return rule.getScaleMin()
                .add(scaleRange.multiply(rawScore).divide(rawMax, 8, RoundingMode.HALF_UP))
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
        // Không có câu đúng nghĩa là kết quả dưới ngưỡng A1. Một số scheme cũ bắt đầu
        // band A1 từ 0, nên chỉ dựa vào band cấu hình sẽ báo sai A1 cho skill bỏ trống hoàn toàn.
        if (scaleScore.signum() == 0) return CefrLevel.BELOW_A1;
        return scoreBandRepository
                .findBySchemeIdAndSkillOrderByMinScoreAsc(schemeId, normalized)
                .stream()
                .filter(
                        band ->
                                scaleScore.compareTo(band.getMinScore()) >= 0
                                        && scaleScore.compareTo(band.getMaxScore()) <= 0)
                .map(streamValue -> streamValue.getCefrLevel())
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
    public BigDecimal overallScale(Integer schemeId, List<ExamSkillResultDTO> skillScores) {
        requireHistoricalScheme(schemeId);
        if (skillScores == null || skillScores.isEmpty()) return null;
        return skillScores.stream()
                .map(streamValue -> streamValue.getScaleScore())
                .filter(streamValue -> Objects.nonNull(streamValue))
                .reduce(BigDecimal.ZERO, (leftValue, rightValue) -> leftValue.add(rightValue))
                .divide(BigDecimal.valueOf(skillScores.size()), 2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    // CEFR tổng hợp lấy skill thấp nhất trong bốn kỹ năng, có điều chỉnh ngưỡng từ G&V.
    public String overallLevel(Integer schemeId, List<ExamSkillResultDTO> skillScores) {
        var certificate = requireHistoricalScheme(schemeId).getCertificate();
        if (skillScores == null || skillScores.isEmpty()) return null;
        return skillScores.stream()
                .map(streamValue -> streamValue.getCefrLevel())
                .filter(streamValue -> Objects.nonNull(streamValue))
                .map(streamValue -> certificate.scoringLevel(streamValue))
                .min(Comparator.comparingInt(streamValue -> levelOrder(streamValue)))
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    // Scoring Aptis tổng hợp không cộng G&V vào điểm bốn kỹ năng, nhưng dùng G&V để xét sát ngưỡng.
    public ExamAttemptDTO fullMockScore(Integer schemeId, List<ExamSkillResultDTO> skillScores) {
        var certificate = requireHistoricalScheme(schemeId).getCertificate();
        if (skillScores == null
                || skillScores.size() != Skill.values().length
                || !skillScores.stream()
                        .map(streamValue -> streamValue.getSkill())
                        .collect(Collectors.toSet())
                        .containsAll(Skill.codes())
                || skillScores.stream()
                        .anyMatch(
                                score ->
                                        score.getScaleScore() == null
                                                || !FULL_MOCK_LEVELS.contains(
                                                        score.getCefrLevel()))) {
            ExamAttemptDTO result = new ExamAttemptDTO();
            result.setSkillResults(skillScores == null ? List.of() : skillScores);
            return result;
        }
        int coreLevel =
                skillScores.stream()
                        .filter(score -> Skill.GRAMMAR_VOCABULARY.name().equals(score.getSkill()))
                        .mapToInt(score -> FULL_MOCK_LEVELS.indexOf(score.getCefrLevel()))
                        .findFirst()
                        .orElseThrow();
        List<ExamSkillResultDTO> adjusted = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int levels = 0;
        for (ExamSkillResultDTO score : skillScores) {
            if (Skill.GRAMMAR_VOCABULARY.name().equals(score.getSkill())) {
                adjusted.add(scoreDto(score.getSkill(), score.getScaleScore(), null));
                continue;
            }
            ScoringScaleRule rule = requireRule(schemeId, score.getSkill());
            BigDecimal range = rule.getScaleMax().subtract(rule.getScaleMin());
            total =
                    total.add(
                            score.getScaleScore()
                                    .subtract(rule.getScaleMin())
                                    .multiply(BigDecimal.valueOf(50))
                                    .divide(range, 8, RoundingMode.HALF_UP));
            int level = borderlineLevel(schemeId, score, coreLevel, range);
            String reportedLevel = certificate.scoringLevel(FULL_MOCK_LEVELS.get(level));
            level = FULL_MOCK_LEVELS.indexOf(reportedLevel);
            levels += level;
            adjusted.add(scoreDto(score.getSkill(), score.getScaleScore(), reportedLevel));
        }
        // Aptis Scoring System v2.1, mục 8: làm tròn trung bình của bốn mức CEFR.
        // Ví dụ được công bố làm tròn điểm giữa lên mức cao hơn.
        int average = Math.round(levels / 4.0f);
        ExamAttemptDTO result = new ExamAttemptDTO();
        result.setFinalScaleScore(total.setScale(2, RoundingMode.HALF_UP));
        result.setEstimatedOverallCefr(FULL_MOCK_LEVELS.get(average));
        result.setSkillResults(List.copyOf(adjusted));
        return result;
    }

    private int borderlineLevel(
            Integer schemeId, ExamSkillResultDTO score, int coreLevel, BigDecimal range) {
        int level = FULL_MOCK_LEVELS.indexOf(score.getCefrLevel());
        if (CefrLevel.BELOW_A1.equals(score.getCefrLevel())) return level;
        // Policy nội bộ dùng cửa sổ một điểm trên thang tương đương 0–50.
        for (ScoreBand band :
                scoreBandRepository.findBySchemeIdAndSkillOrderByMinScoreAsc(
                        schemeId, score.getSkill())) {
            int next = FULL_MOCK_LEVELS.indexOf(band.getCefrLevel());
            BigDecimal gap = band.getMinScore().subtract(score.getScaleScore());
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

    private ExamSkillResultDTO scoreDto(String skill, BigDecimal scaleScore, String cefrLevel) {
        ExamSkillResultDTO result = new ExamSkillResultDTO();
        result.setSkill(skill);
        result.setScaleScore(scaleScore);
        result.setCefrLevel(cefrLevel);
        return result;
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

    /** Attempt lịch sử tiếp tục dùng scheme bất biến của nó dù scheme đã ngừng sử dụng. */
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

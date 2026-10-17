package com.aptisprep.service.scoring;

import com.aptisprep.entity.ScoreBand;
import com.aptisprep.entity.ScoringScheme;
import com.aptisprep.enums.RecordStatus;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.ScoreBandRepository;
import com.aptisprep.repository.ScoringSchemeRepository;
import com.aptisprep.service.contract.ScoringPolicyContract;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Estimated internal policy. Raw input is defined as a 0..50 skill score; no official Aptis claim. */
@Service
public class ScoringPolicyService implements ScoringPolicyContract {
    private final ScoringSchemeRepository schemes;
    private final ScoreBandRepository bands;
    public ScoringPolicyService(ScoringSchemeRepository schemes, ScoreBandRepository bands) { this.schemes = schemes; this.bands = bands; }

    @Override @Transactional(readOnly = true)
    public BigDecimal scale(Integer schemeId, String skill, BigDecimal rawScore) {
        requireScheme(schemeId); normalizeSkill(skill);
        if (rawScore == null) return null;
        if (rawScore.compareTo(BigDecimal.ZERO) < 0 || rawScore.compareTo(BigDecimal.valueOf(50)) > 0) {
            throw new BusinessRuleException("Điểm raw phải trong khoảng 0 đến 50");
        }
        // The V2 schema has no raw-to-scale table. Version 1 deliberately keeps the declared 0..50 scale.
        return rawScore.setScale(2, RoundingMode.HALF_UP);
    }

    @Override @Transactional(readOnly = true)
    public String estimatedLevel(Integer schemeId, String skill, BigDecimal scaleScore) {
        requireScheme(schemeId); String normalized = normalizeSkill(skill);
        if (scaleScore == null) return null;
        if (scaleScore.compareTo(BigDecimal.ZERO) < 0 || scaleScore.compareTo(BigDecimal.valueOf(50)) > 0) {
            throw new BusinessRuleException("Điểm scale phải trong khoảng 0 đến 50");
        }
        return bands.findBySchemeIdAndSkillOrderByMinScoreAsc(schemeId, normalized).stream()
                .filter(band -> scaleScore.compareTo(band.getMinScore()) >= 0 && scaleScore.compareTo(band.getMaxScore()) <= 0)
                .map(ScoreBand::getCefrLevel).findFirst().orElse(null);
    }

    private ScoringScheme requireScheme(Integer id) {
        if (id == null) throw new BusinessRuleException("Thiếu phiên bản bộ chấm điểm");
        ScoringScheme scheme = schemes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ chấm điểm"));
        if (scheme.getStatus() != RecordStatus.ACTIVE || scheme.getCertificate().getStatus() != RecordStatus.ACTIVE) {
            throw new BusinessRuleException("Bộ chấm điểm hoặc chứng chỉ đã ngừng sử dụng");
        }
        return scheme;
    }
    private String normalizeSkill(String skill) {
        if (skill == null) throw new BusinessRuleException("Thiếu kỹ năng");
        String value = skill.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        try { com.aptisprep.enums.Skill.valueOf(value); return value; }
        catch (IllegalArgumentException exception) { throw new BusinessRuleException("Kỹ năng không hợp lệ"); }
    }
}

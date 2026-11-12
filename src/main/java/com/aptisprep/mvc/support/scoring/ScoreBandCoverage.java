package com.aptisprep.mvc.support.scoring;

import com.aptisprep.mvc.entity.ScoreBand;
import com.aptisprep.mvc.entity.ScoringScaleRule;
import com.aptisprep.mvc.enums.CefrLevel;
import com.aptisprep.mvc.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.util.List;

public final class ScoreBandCoverage {
    private static final BigDecimal SCORE_STEP = new BigDecimal("0.01");

    private ScoreBandCoverage() {}

    public static void requireComplete(ScoringScaleRule rule, List<ScoreBand> bands) {
        if (rule == null
                || rule.getScaleMin() == null
                || rule.getScaleMax() == null
                || rule.getScaleMin().compareTo(rule.getScaleMax()) >= 0
                || bands == null
                || bands.isEmpty()) {
            throw new BusinessRuleException("Các khoảng CEFR chưa phủ đủ miền điểm scale");
        }
        BigDecimal expected = rule.getScaleMin();
        ScoreBand previous = null;
        for (ScoreBand band : bands) {
            if (band == null
                    || CefrLevel.parse(band.getCefrLevel()) == null
                    || band.getMinScore() == null
                    || band.getMaxScore() == null
                    || band.getMinScore().compareTo(band.getMaxScore()) > 0
                    || band.getMinScore().compareTo(rule.getScaleMin()) < 0
                    || band.getMaxScore().compareTo(rule.getScaleMax()) > 0
                    || band.getMinScore().compareTo(expected) > 0
                    || (previous != null
                            && band.getMinScore().compareTo(previous.getMaxScore()) <= 0)) {
                throw new BusinessRuleException(
                        "Các khoảng CEFR bị thiếu hoặc chồng lấn trong miền điểm scale");
            }
            if (previous != null
                    && !CefrLevel.parse(band.getCefrLevel())
                            .higherThan(CefrLevel.parse(previous.getCefrLevel())))
                throw new BusinessRuleException(
                        "CEFR phải tăng dần theo điểm: A1, A2, B1, B2, C1, C2");
            expected = band.getMaxScore().add(SCORE_STEP);
            previous = band;
        }
        if (previous.getMaxScore().compareTo(rule.getScaleMax()) < 0) {
            throw new BusinessRuleException("Các khoảng CEFR chưa phủ đủ miền điểm scale");
        }
    }
}

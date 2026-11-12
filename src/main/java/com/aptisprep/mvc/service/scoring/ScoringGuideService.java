package com.aptisprep.mvc.service.scoring;

import com.aptisprep.mvc.support.contract.IntegrationModels.AttemptResult;
import com.aptisprep.mvc.support.contract.IntegrationModels.SkillResult;
import java.math.BigDecimal;
import java.util.List;

public interface ScoringGuideService {
    record Band(BigDecimal min, BigDecimal max, String level, boolean attained) {}

    record Guide(String skill, String version, BigDecimal min, BigDecimal max, List<Band> bands) {}

    record ResultGuideView(List<Guide> guides, List<SkillResult> feedbackWithoutGuide) {
        public BigDecimal combinedScaleMax() {
            return guides.stream()
                    .filter(guide -> !"GRAMMAR_VOCABULARY".equals(guide.skill()))
                    .map(Guide::max)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    public ResultGuideView forResultView(AttemptResult result);

    public List<Guide> forExam(Integer examId);

    public List<Guide> forResult(AttemptResult result);
}

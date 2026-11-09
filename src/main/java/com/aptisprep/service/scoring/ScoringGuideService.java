package com.aptisprep.service.scoring;

import com.aptisprep.service.contract.IntegrationModels.AttemptResult;
import com.aptisprep.service.contract.IntegrationModels.SkillResult;
import java.math.BigDecimal;
import java.util.List;

public interface ScoringGuideService {
    record Band(BigDecimal min, BigDecimal max, String level, boolean attained) {}

    record Guide(
            String skill, String version, BigDecimal min, BigDecimal max, List<Band> bands) {}

    record ResultGuideView(List<Guide> guides, List<SkillResult> feedbackWithoutGuide) {}

    ResultGuideView forResultView(AttemptResult result);
    List<Guide> forExam(Integer examId);
    List<Guide> forResult(AttemptResult result);
}

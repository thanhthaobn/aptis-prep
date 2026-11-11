package com.aptisprep.service.scoring;

import com.aptisprep.presentation.AttemptResultDTO;
import com.aptisprep.presentation.SkillResultDTO;
import java.math.BigDecimal;
import java.util.List;

public interface ScoringGuideService {
    record Band(BigDecimal min, BigDecimal max, String level, boolean attained) {}

    record Guide(String skill, String version, BigDecimal min, BigDecimal max, List<Band> bands) {}

    record ResultGuideView(List<Guide> guides, List<SkillResultDTO> feedbackWithoutGuide) {}

    ResultGuideView forResultView(AttemptResultDTO result);

    List<Guide> forExam(Integer examId);

    List<Guide> forResult(AttemptResultDTO result);
}

package com.aptisprep.service.scoring;

import com.aptisprep.dto.AttemptResultDTO;
import com.aptisprep.dto.SkillResultDTO;
import java.math.BigDecimal;
import java.util.List;

public interface ScoringGuideService {
    public record Band(BigDecimal min, BigDecimal max, String level, boolean attained) {}

    public record Guide(
            String skill, String version, BigDecimal min, BigDecimal max, List<Band> bands) {}

    public record ResultGuideView(List<Guide> guides, List<SkillResultDTO> feedbackWithoutGuide) {}

    public ResultGuideView forResultView(AttemptResultDTO result);

    public List<Guide> forExam(Integer examId);

    public List<Guide> forResult(AttemptResultDTO result);
}

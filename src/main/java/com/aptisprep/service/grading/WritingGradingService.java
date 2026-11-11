package com.aptisprep.service.grading;

import com.aptisprep.dto.GradingResultDTO;

public interface WritingGradingService {
    public GradingResultDTO grade(String prompt, String answer);
}

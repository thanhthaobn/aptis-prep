package com.aptisprep.service.grading;

import com.aptisprep.dto.view.GradingResultDTO;

public interface WritingGradingService {
    GradingResultDTO grade(String prompt, String answer);
}

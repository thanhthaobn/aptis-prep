package com.aptisprep.service.grading;

import com.aptisprep.presentation.GradingResultDTO;

public interface WritingGradingService {
    GradingResultDTO grade(String prompt, String answer);
}

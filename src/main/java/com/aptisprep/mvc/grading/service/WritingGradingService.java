package com.aptisprep.mvc.grading.service;

import com.aptisprep.mvc.grading.dto.GradingResultDTO;

public interface WritingGradingService {
    GradingResultDTO grade(String prompt, String answer);
}

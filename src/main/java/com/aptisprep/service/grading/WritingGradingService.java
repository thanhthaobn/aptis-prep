package com.aptisprep.service.grading;

public interface WritingGradingService {

    GradingResult grade(String prompt, String answer);
}

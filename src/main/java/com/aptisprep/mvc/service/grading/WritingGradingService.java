package com.aptisprep.mvc.service.grading;

import com.aptisprep.mvc.support.grading.GradingResult;

public interface WritingGradingService {

    public GradingResult grade(String prompt, String answer);
}

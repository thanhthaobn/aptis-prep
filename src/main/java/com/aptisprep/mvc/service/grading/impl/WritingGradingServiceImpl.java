package com.aptisprep.mvc.service.grading.impl;

import com.aptisprep.mvc.service.grading.GeminiGradingService;
import com.aptisprep.mvc.service.grading.WritingGradingService;
import com.aptisprep.mvc.support.grading.*;
import com.aptisprep.mvc.support.grading.GradingResult;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class WritingGradingServiceImpl implements WritingGradingService {
    private GeminiGradingService delegate;

    public WritingGradingServiceImpl(GeminiGradingService delegate) {
        this.delegate = delegate;
    }

    @Override
    public GradingResult grade(String prompt, String answer) {
        return delegate.grade(prompt, answer);
    }
}

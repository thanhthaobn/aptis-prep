package com.aptisprep.mvc.service.grading.impl;
import com.aptisprep.mvc.support.grading.*;

import com.aptisprep.mvc.support.grading.GradingResult;
import com.aptisprep.mvc.service.grading.GeminiGradingService;
import com.aptisprep.mvc.service.grading.SpeakingGradingService;
import java.nio.file.Path;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class SpeakingGradingServiceImpl implements SpeakingGradingService {
    private GeminiGradingService delegate;

    public SpeakingGradingServiceImpl(GeminiGradingService delegate) {
        this.delegate = delegate;
    }

    @Override
    public GradingResult grade(String prompt, Path audioFile) {
        return delegate.grade(prompt, audioFile);
    }

    @Override
    public String transcribe(Path audioFile) {
        return delegate.transcribe(audioFile);
    }

    @Override
    public GradingResult gradeTranscript(String prompt, String transcript) {
        return delegate.gradeTranscript(prompt, transcript);
    }
}

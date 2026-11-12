package com.aptisprep.mvc.service.grading;

import com.aptisprep.mvc.support.grading.GradingResult;
import java.nio.file.Path;

/** Adapts Gemini for writing, speaking, and transcription grading. */
public interface GeminiGradingService extends WritingGradingService, SpeakingGradingService {
    GradingResult grade(String prompt, String answer);

    GradingResult grade(String prompt, Path audioFile);

    String transcribe(Path audioFile);

    GradingResult gradeTranscript(String prompt, String transcript);
}

package com.aptisprep.mvc.grading.service;

import com.aptisprep.mvc.grading.dto.GradingResultDTO;
import java.nio.file.Path;

public interface SpeakingGradingService {
    GradingResultDTO grade(String prompt, Path audioFile);

    String transcribe(Path audioFile);

    GradingResultDTO gradeTranscript(String prompt, String transcript);
}

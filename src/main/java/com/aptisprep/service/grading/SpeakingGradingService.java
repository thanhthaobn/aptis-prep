package com.aptisprep.service.grading;

import com.aptisprep.dto.view.GradingResultDTO;
import java.nio.file.Path;

public interface SpeakingGradingService {
    GradingResultDTO grade(String prompt, Path audioFile);

    String transcribe(Path audioFile);

    GradingResultDTO gradeTranscript(String prompt, String transcript);
}

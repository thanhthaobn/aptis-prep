package com.aptisprep.service.grading;

import com.aptisprep.dto.GradingResultDTO;
import java.nio.file.Path;

public interface SpeakingGradingService {
    public GradingResultDTO grade(String prompt, Path audioFile);

    public String transcribe(Path audioFile);

    public GradingResultDTO gradeTranscript(String prompt, String transcript);
}

package com.aptisprep.service.grading;

import java.nio.file.Path;

public interface SpeakingGradingService {

    GradingResult grade(String prompt, Path audioFile);

    GradingResult gradeTranscript(String prompt, String transcript);
}

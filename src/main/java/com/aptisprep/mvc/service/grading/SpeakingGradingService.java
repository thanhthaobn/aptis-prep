package com.aptisprep.mvc.service.grading;

import com.aptisprep.mvc.support.grading.GradingResult;
import java.nio.file.Path;

public interface SpeakingGradingService {

    public GradingResult grade(String prompt, Path audioFile);

    public String transcribe(Path audioFile);

    public GradingResult gradeTranscript(String prompt, String transcript);
}

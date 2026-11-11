package com.aptisprep.service.grading;

import com.aptisprep.presentation.AiAssessmentDTO.AssessmentRequest;
import com.aptisprep.presentation.AiAssessmentDTO.AssessmentView;

/** Server-to-server contract; frontend never supplies CEFR, grading context or provider data. */
public interface AiAssessmentService {
    AssessmentView request(AssessmentRequest request);

    AssessmentView process(String assessmentKey);

    AssessmentView retry(String assessmentKey, Integer requesterId, boolean admin);

    AssessmentView get(String assessmentKey, Integer requesterId, boolean admin);

    /** Internal grading callback read; never expose this method from a controller. */
    AssessmentView getForGrading(String assessmentKey);
}

package com.aptisprep.service.grading;

import com.aptisprep.dto.AiAssessmentDTO.AssessmentRequest;
import com.aptisprep.dto.AiAssessmentDTO.AssessmentView;

/** Server-to-server contract; frontend never supplies CEFR, grading context or provider data. */
public interface AiAssessmentService {
    public AssessmentView request(AssessmentRequest request);

    public AssessmentView process(String assessmentKey);

    public AssessmentView retry(String assessmentKey, Integer requesterId, boolean admin);

    public AssessmentView get(String assessmentKey, Integer requesterId, boolean admin);

    /** Internal grading callback read; never expose this method from a controller. */
    public AssessmentView getForGrading(String assessmentKey);
}

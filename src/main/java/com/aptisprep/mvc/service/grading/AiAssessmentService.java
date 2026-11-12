package com.aptisprep.mvc.service.grading;

import com.aptisprep.mvc.dto.AiAssessmentDTO;
import com.aptisprep.mvc.dto.AiAssessmentJobDTO;

/** Coordinates asynchronous AI assessment requests and recovery. */
public interface AiAssessmentService {
    public AiAssessmentDTO request(AiAssessmentJobDTO request);

    public AiAssessmentDTO process(String assessmentKey);

    public AiAssessmentDTO retry(String assessmentKey, Integer requesterId, boolean admin);

    public AiAssessmentDTO get(String assessmentKey, Integer requesterId, boolean admin);

    public AiAssessmentDTO getForGrading(String assessmentKey);
    public void recoverAiJobs();
}








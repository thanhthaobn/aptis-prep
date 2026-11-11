package com.aptisprep.mvc.grading.repository;

import com.aptisprep.mvc.grading.entity.AiAssessment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiAssessmentRepository extends JpaRepository<AiAssessment, Integer> {
    Optional<AiAssessment> findFirstByJobIdAndAnswerVersionOrderByIdDesc(
            Integer jobId, Integer answerVersion);

    Optional<AiAssessment> findFirstByAssessmentKeyOrderByIdDesc(String assessmentKey);
}

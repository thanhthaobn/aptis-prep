package com.aptisprep.mvc.repository;

import com.aptisprep.mvc.entity.AiAssessment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiAssessmentRepository extends JpaRepository<AiAssessment, Integer> {
    Optional<AiAssessment> findFirstByJobIdAndAnswerVersionOrderByIdDesc(
            Integer jobId, Integer answerVersion);

    Optional<AiAssessment> findFirstByAssessmentKeyOrderByIdDesc(String assessmentKey);
}

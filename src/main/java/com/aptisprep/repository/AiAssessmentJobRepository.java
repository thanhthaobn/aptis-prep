package com.aptisprep.repository;

import com.aptisprep.entity.AiAssessmentJob;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiAssessmentJobRepository extends JpaRepository<AiAssessmentJob, Integer> {
    Optional<AiAssessmentJob> findByAssessmentKey(String assessmentKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from AiAssessmentJob job where job.assessmentKey = :assessmentKey")
    Optional<AiAssessmentJob> findByAssessmentKeyForUpdate(@Param("assessmentKey") String assessmentKey);
}

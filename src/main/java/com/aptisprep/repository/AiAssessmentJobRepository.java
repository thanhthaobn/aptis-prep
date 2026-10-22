package com.aptisprep.repository;

import com.aptisprep.entity.AiAssessmentJob;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.List;

import com.aptisprep.enums.AiJobState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiAssessmentJobRepository extends JpaRepository<AiAssessmentJob, Integer> {
    Optional<AiAssessmentJob> findByAssessmentKey(String assessmentKey);

    @Query("""
            select job from AiAssessmentJob job
            where job.sourceType = 'EXAM_ANSWER'
              and job.userId = :userId
              and job.attemptId = :attemptId
              and job.answerVersion = :answerVersion
              and job.policyVersion = :policyVersion
              and job.rubricVersion = :rubricVersion
            order by job.id asc
            """)
    List<AiAssessmentJob> findExamJobs(@Param("userId") Integer userId, @Param("attemptId") Integer attemptId,
            @Param("answerVersion") Integer answerVersion, @Param("policyVersion") String policyVersion,
            @Param("rubricVersion") String rubricVersion);

    @Query("""
            select job from AiAssessmentJob job
            where job.sourceType = 'EXAM_ANSWER'
              and job.userId = :userId
              and job.attemptId = :attemptId
              and job.answerVersion = :answerVersion
              and job.policyVersion = :policyVersion
              and job.rubricVersion = :rubricVersion
              and job.state = :state
            order by job.id asc
            """)
    List<AiAssessmentJob> findExamJobsByState(@Param("userId") Integer userId, @Param("attemptId") Integer attemptId,
            @Param("answerVersion") Integer answerVersion, @Param("policyVersion") String policyVersion,
            @Param("rubricVersion") String rubricVersion, @Param("state") AiJobState state);

    List<AiAssessmentJob> findTop50ByStateAndUpdatedAtBeforeOrderByUpdatedAtAsc(AiJobState state, java.time.LocalDateTime time);
    List<AiAssessmentJob> findTop50BySourceTypeAndStateOrderByUpdatedAtAsc(String sourceType, AiJobState state);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from AiAssessmentJob job where job.assessmentKey = :assessmentKey")
    Optional<AiAssessmentJob> findByAssessmentKeyForUpdate(@Param("assessmentKey") String assessmentKey);
}

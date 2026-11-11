package com.aptisprep.repository;

import com.aptisprep.entity.AiAssessmentJob;
import com.aptisprep.enums.AiJobState;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiAssessmentJobRepository extends JpaRepository<AiAssessmentJob, Integer> {
    Optional<AiAssessmentJob> findByAssessmentKey(String assessmentKey);

    @Query(
            """
            select job from AiAssessmentJob job
            where job.sourceType = 'EXAM_ANSWER'
              and job.userId = :userId
              and job.attemptId = :attemptId
              and job.answerVersion = :answerVersion
              and job.policyVersion = :policyVersion
              and job.rubricVersion = :rubricVersion
            order by job.id asc
            """)
    List<AiAssessmentJob> findExamJobs(
            @Param("userId") Integer userId,
            @Param("attemptId") Integer attemptId,
            @Param("answerVersion") Integer answerVersion,
            @Param("policyVersion") String policyVersion,
            @Param("rubricVersion") String rubricVersion);

    @Query(
            """
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
    List<AiAssessmentJob> findExamJobsByState(
            @Param("userId") Integer userId,
            @Param("attemptId") Integer attemptId,
            @Param("answerVersion") Integer answerVersion,
            @Param("policyVersion") String policyVersion,
            @Param("rubricVersion") String rubricVersion,
            @Param("state") AiJobState state);

    @Query(
            """
            select job from AiAssessmentJob job
            where job.sourceType = 'EXAM_ANSWER'
              and job.userId = :userId
              and job.attemptId = :attemptId
              and job.state = :state
            order by job.id asc
            """)
    List<AiAssessmentJob> findExamJobsByAttemptAndState(
            @Param("userId") Integer userId,
            @Param("attemptId") Integer attemptId,
            @Param("state") AiJobState state);

    @Query(
            """
            select job.assessmentKey from AiAssessmentJob job
            where (job.state = :pending and (job.nextRetryAt is null or job.nextRetryAt <= :now))
               or (job.state = :processing and (job.updatedAt is null or job.updatedAt < :cutoff))
            order by coalesce(job.nextRetryAt, job.updatedAt, job.createdAt), job.id
            """)
    List<String> findRecoverableKeys(
            @Param("pending") AiJobState pending,
            @Param("processing") AiJobState processing,
            @Param("now") LocalDateTime now,
            @Param("cutoff") LocalDateTime cutoff,
            Pageable page);

    @Query(
            """
select job.assessmentKey from AiAssessmentJob job
where job.sourceType = 'EXAM_ANSWER'
  and job.state = com.aptisprep.enums.AiJobState.COMPLETED
  and job.attemptId is not null and job.answerId is not null
  and exists (select a.id from AiAssessment a where a.job.id = job.id
      and a.answerVersion = job.answerVersion
      and a.state = com.aptisprep.enums.AiAssessmentState.COMPLETED and a.score is not null)
  and not exists (select o.id from com.aptisprep.entity.AttemptOutboxEntity o
      where o.eventKey = concat('ai:', job.assessmentKey, ':v', cast(job.answerVersion as string), ':COMPLETED'))
order by job.id
""")
    List<String> findCompletedWithoutCallback(Pageable page);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from AiAssessmentJob job where job.assessmentKey = :assessmentKey")
    Optional<AiAssessmentJob> findByAssessmentKeyForUpdate(
            @Param("assessmentKey") String assessmentKey);
}

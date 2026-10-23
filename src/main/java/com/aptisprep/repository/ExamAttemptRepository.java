package com.aptisprep.repository;

import com.aptisprep.entity.ExamAttemptEntity;
import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamAttemptRepository extends JpaRepository<ExamAttemptEntity, Integer> {
    boolean existsByScoringSchemeId(Integer scoringSchemeId);
    Optional<ExamAttemptEntity> findByIdAndUserId(Integer id, Integer userId);

    Optional<ExamAttemptEntity> findByUserIdAndRequestKey(Integer userId, String requestKey);

    Page<ExamAttemptEntity> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    List<ExamAttemptEntity> findByUserId(Integer userId);

    @Query(value = "select a from ExamAttemptEntity a join MockExamEntity e on e.id = a.mockExamId "
            + "where a.userId = :userId "
            + "and (:pattern = '' or lower(e.title) like :pattern) "
            + "and (:examType = '' or e.examType = :examType) "
            + "and (:state = '' or a.status = :state) "
            + "and (:skill = '' or e.skill = :skill or exists (select r.id from ExamSkillResultEntity r where r.attemptId = a.id and r.skill = :skill)) "
            + "order by a.createdAt desc",
            countQuery = "select count(a) from ExamAttemptEntity a join MockExamEntity e on e.id = a.mockExamId "
                    + "where a.userId = :userId "
                    + "and (:pattern = '' or lower(e.title) like :pattern) "
                    + "and (:examType = '' or e.examType = :examType) "
                    + "and (:state = '' or a.status = :state) "
                    + "and (:skill = '' or e.skill = :skill or exists (select r.id from ExamSkillResultEntity r where r.attemptId = a.id and r.skill = :skill))")
    Page<ExamAttemptEntity> searchHistory(@Param("userId") Integer userId, @Param("pattern") String pattern,
                                          @Param("skill") String skill, @Param("examType") String examType, @Param("state") String state,
                                          Pageable pageable);

    @Query(value = "select a from ExamAttemptEntity a join MockExamEntity e on e.id = a.mockExamId join User u on u.id = a.userId "
            + "where (:pattern = '' or lower(e.title) like :pattern or lower(u.fullName) like :pattern or lower(u.email) like :pattern) "
            + "and (:examType = '' or e.examType = :examType) and (:state = '' or a.status = :state) "
            + "and (:skill = '' or e.skill = :skill or exists (select r.id from ExamSkillResultEntity r where r.attemptId = a.id and r.skill = :skill)) "
            + "and (:submittedFrom is null or a.submittedAt >= :submittedFrom) and (:submittedTo is null or a.submittedAt < :submittedTo) order by a.createdAt desc",
            countQuery = "select count(a) from ExamAttemptEntity a join MockExamEntity e on e.id = a.mockExamId join User u on u.id = a.userId "
                    + "where (:pattern = '' or lower(e.title) like :pattern or lower(u.fullName) like :pattern or lower(u.email) like :pattern) "
                    + "and (:examType = '' or e.examType = :examType) and (:state = '' or a.status = :state) "
                    + "and (:skill = '' or e.skill = :skill or exists (select r.id from ExamSkillResultEntity r where r.attemptId = a.id and r.skill = :skill)) "
                    + "and (:submittedFrom is null or a.submittedAt >= :submittedFrom) and (:submittedTo is null or a.submittedAt < :submittedTo)")
    Page<ExamAttemptEntity> searchAllHistory(@Param("pattern") String pattern, @Param("skill") String skill,
                                             @Param("examType") String examType, @Param("state") String state,
                                             @Param("submittedFrom") java.time.LocalDateTime submittedFrom, @Param("submittedTo") java.time.LocalDateTime submittedTo,
                                             Pageable pageable);

    long countByUserId(Integer userId);

    boolean existsByMockExamId(Integer mockExamId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ExamAttemptEntity a where a.id = :id and a.userId = :userId")
    Optional<ExamAttemptEntity> findByIdAndUserIdForUpdate(@Param("id") Integer id, @Param("userId") Integer userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ExamAttemptEntity a join MockExamEntity e on e.id = a.mockExamId "
            + "where a.userId = :userId and e.examType = 'PLACEMENT' and e.skill = :skill and a.status in :states")
    Optional<ExamAttemptEntity> findActivePlacement(@Param("userId") Integer userId, @Param("skill") String skill,
                                                    @Param("states") Collection<String> states);
}

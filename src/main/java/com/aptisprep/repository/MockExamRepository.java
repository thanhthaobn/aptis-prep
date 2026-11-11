package com.aptisprep.repository;

import com.aptisprep.entity.MockExamEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface MockExamRepository extends JpaRepository<MockExamEntity, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from MockExamEntity e where e.id = :id")
    Optional<MockExamEntity> findByIdForUpdate(Integer id);

    boolean existsByScoringSchemeId(Integer scoringSchemeId);

    @Query(
            "select distinct e.scoringSchemeId as schemeId, e.examType as examType from MockExamEntity e where e.scoringSchemeId is not null")
    List<SchemeExamType> findScoringSchemeExamTypes();

    interface SchemeExamType {
        Integer getSchemeId();

        String getExamType();
    }

    long countByStatus(String status);

    List<MockExamEntity> findByStatusOrderByUpdatedAtDesc(String status);

    List<MockExamEntity> findBySkillAndStatusOrderByUpdatedAtDesc(String skill, String status);
}

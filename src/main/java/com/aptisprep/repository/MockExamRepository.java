package com.aptisprep.repository;

import com.aptisprep.entity.MockExamEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MockExamRepository extends JpaRepository<MockExamEntity, Integer> {
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

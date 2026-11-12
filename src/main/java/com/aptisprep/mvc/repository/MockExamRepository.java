package com.aptisprep.mvc.repository;

import com.aptisprep.mvc.entity.MockExam;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface MockExamRepository extends JpaRepository<MockExam, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from MockExam e where e.id = :id")
    Optional<MockExam> findByIdForUpdate(Integer id);

    boolean existsByScoringSchemeId(Integer scoringSchemeId);

    @Query(
            "select distinct e.scoringSchemeId as schemeId, e.examType as examType from MockExam e where e.scoringSchemeId is not null")
    List<SchemeExamType> findScoringSchemeExamTypes();

    interface SchemeExamType {
        Integer getSchemeId();

        String getExamType();
    }

    long countByStatus(String status);

    List<MockExam> findByStatusOrderByUpdatedAtDesc(String status);

    List<MockExam> findBySkillAndStatusOrderByUpdatedAtDesc(String skill, String status);
}

package com.aptisprep.mvc.repository;

import com.aptisprep.mvc.entity.ExamAttemptSnapshot;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamAttemptSnapshotRepository extends JpaRepository<ExamAttemptSnapshot, Integer> {
    Optional<ExamAttemptSnapshot> findByAttemptId(Integer attemptId);
}

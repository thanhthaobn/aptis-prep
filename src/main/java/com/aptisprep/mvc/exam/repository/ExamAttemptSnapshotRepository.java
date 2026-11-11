package com.aptisprep.mvc.exam.repository;

import com.aptisprep.mvc.exam.entity.ExamAttemptSnapshotEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamAttemptSnapshotRepository
        extends JpaRepository<ExamAttemptSnapshotEntity, Integer> {
    Optional<ExamAttemptSnapshotEntity> findByAttemptId(Integer attemptId);
}

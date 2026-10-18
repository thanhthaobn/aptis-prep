package com.aptisprep.repository;

import com.aptisprep.entity.MockExamEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MockExamRepository extends JpaRepository<MockExamEntity, Integer> {
    List<MockExamEntity> findByStatusOrderByUpdatedAtDesc(String status);
    List<MockExamEntity> findBySkillAndStatusOrderByUpdatedAtDesc(String skill, String status);
}

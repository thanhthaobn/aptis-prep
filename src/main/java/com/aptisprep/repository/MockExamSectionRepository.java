package com.aptisprep.repository;

import com.aptisprep.entity.MockExamSectionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MockExamSectionRepository extends JpaRepository<MockExamSectionEntity, Integer> {
    List<MockExamSectionEntity> findByMockExamIdOrderById(Integer mockExamId);

    void deleteByMockExamId(Integer mockExamId);
}

package com.aptisprep.mvc.catalog.repository;

import com.aptisprep.mvc.catalog.entity.MockExamSectionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MockExamSectionRepository extends JpaRepository<MockExamSectionEntity, Integer> {
    List<MockExamSectionEntity> findByMockExamIdOrderById(Integer mockExamId);

    void deleteByMockExamId(Integer mockExamId);
}

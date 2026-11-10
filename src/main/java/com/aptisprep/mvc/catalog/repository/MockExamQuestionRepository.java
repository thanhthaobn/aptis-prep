package com.aptisprep.mvc.catalog.repository;

import com.aptisprep.mvc.catalog.entity.MockExamQuestionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MockExamQuestionRepository extends JpaRepository<MockExamQuestionEntity, Integer> {
    List<MockExamQuestionEntity> findByMockExamIdOrderByPosition(Integer mockExamId);

    void deleteByMockExamId(Integer mockExamId);
}

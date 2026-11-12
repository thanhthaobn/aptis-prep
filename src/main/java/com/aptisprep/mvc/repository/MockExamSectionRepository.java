package com.aptisprep.mvc.repository;

import com.aptisprep.mvc.entity.MockExamSection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MockExamSectionRepository extends JpaRepository<MockExamSection, Integer> {
    List<MockExamSection> findByMockExamIdOrderById(Integer mockExamId);

    void deleteByMockExamId(Integer mockExamId);
}








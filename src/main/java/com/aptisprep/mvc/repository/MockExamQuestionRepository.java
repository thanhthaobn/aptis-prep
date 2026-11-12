package com.aptisprep.mvc.repository;

import com.aptisprep.mvc.entity.MockExamQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MockExamQuestionRepository extends JpaRepository<MockExamQuestion, Integer> {
    List<MockExamQuestion> findByMockExamIdOrderByPosition(Integer mockExamId);

    void deleteByMockExamId(Integer mockExamId);
}








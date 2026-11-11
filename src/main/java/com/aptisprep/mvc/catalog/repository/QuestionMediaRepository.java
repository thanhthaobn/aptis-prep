package com.aptisprep.mvc.catalog.repository;

import com.aptisprep.mvc.catalog.entity.QuestionMediaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionMediaRepository extends JpaRepository<QuestionMediaEntity, Integer> {
    List<QuestionMediaEntity> findByQuestionIdOrderByPosition(Integer questionId);
}

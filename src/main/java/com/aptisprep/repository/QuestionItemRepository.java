package com.aptisprep.repository;

import com.aptisprep.entity.QuestionItemEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionItemRepository extends JpaRepository<QuestionItemEntity, Integer> {
    List<QuestionItemEntity> findByQuestionIdOrderByPosition(Integer questionId);
    void deleteByQuestionId(Integer questionId);
}

package com.aptisprep.repository;

import com.aptisprep.entity.QuestionOptionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionOptionRepository extends JpaRepository<QuestionOptionEntity, Integer> {
    List<QuestionOptionEntity> findByQuestionItemIdOrderByPosition(Integer questionItemId);

    void deleteByQuestionItemId(Integer questionItemId);
}

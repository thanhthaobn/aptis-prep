package com.aptisprep.mvc.repository;

import com.aptisprep.mvc.entity.QuestionItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionItemRepository extends JpaRepository<QuestionItem, Integer> {
    List<QuestionItem> findByQuestionIdOrderByPosition(Integer questionId);

    void deleteByQuestionId(Integer questionId);
}

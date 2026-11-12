package com.aptisprep.mvc.repository;

import com.aptisprep.mvc.entity.QuestionOption;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Integer> {
    List<QuestionOption> findByQuestionItemIdOrderByPosition(Integer questionItemId);

    void deleteByQuestionItemId(Integer questionItemId);
}

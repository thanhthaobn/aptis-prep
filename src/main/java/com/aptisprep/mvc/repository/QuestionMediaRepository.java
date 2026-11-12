package com.aptisprep.mvc.repository;

import com.aptisprep.mvc.entity.QuestionMedia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionMediaRepository extends JpaRepository<QuestionMedia, Integer> {
    List<QuestionMedia> findByQuestionIdOrderByPosition(Integer questionId);
}








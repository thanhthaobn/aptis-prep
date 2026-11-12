package com.aptisprep.mvc.repository;

import com.aptisprep.mvc.entity.Question;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Integer> {
    List<Question> findByStatusOrderByUpdatedAtDesc(String status);

    List<Question> findBySkillAndStatusOrderByUpdatedAtDesc(String skill, String status);
}

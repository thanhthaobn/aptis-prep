package com.aptisprep.mvc.catalog.repository;

import com.aptisprep.mvc.catalog.entity.QuestionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<QuestionEntity, Integer> {
    List<QuestionEntity> findByStatusOrderByUpdatedAtDesc(String status);

    List<QuestionEntity> findBySkillAndStatusOrderByUpdatedAtDesc(String skill, String status);
}

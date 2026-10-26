package com.aptisprep.repository;

import com.aptisprep.entity.UserAnswerDetailEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAnswerDetailRepository extends JpaRepository<UserAnswerDetailEntity, Integer> {
    List<UserAnswerDetailEntity> findByUserAnswerIdOrderByQuestionItemIdAsc(Integer answerId);

    Optional<UserAnswerDetailEntity> findByUserAnswerIdAndQuestionItemId(
            Integer answerId, Integer itemId);
}

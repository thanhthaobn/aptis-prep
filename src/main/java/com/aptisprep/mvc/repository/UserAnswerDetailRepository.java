package com.aptisprep.mvc.repository;

import com.aptisprep.mvc.entity.UserAnswerDetail;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAnswerDetailRepository extends JpaRepository<UserAnswerDetail, Integer> {
    List<UserAnswerDetail> findByUserAnswerIdOrderByQuestionItemIdAsc(Integer answerId);

    Optional<UserAnswerDetail> findByUserAnswerIdAndQuestionItemId(
            Integer answerId, Integer itemId);
}

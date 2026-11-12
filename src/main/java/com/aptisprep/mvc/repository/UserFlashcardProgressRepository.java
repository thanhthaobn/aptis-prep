package com.aptisprep.mvc.repository;

import com.aptisprep.mvc.entity.UserFlashcardProgress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFlashcardProgressRepository
        extends JpaRepository<UserFlashcardProgress, Integer> {
    Optional<UserFlashcardProgress> findByUserIdAndFlashcardId(Integer userId, Integer flashcardId);

    List<UserFlashcardProgress> findByUserId(Integer userId);
}

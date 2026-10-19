package com.aptisprep.repository;
import com.aptisprep.entity.UserFlashcardProgress;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserFlashcardProgressRepository extends JpaRepository<UserFlashcardProgress, Integer> {
    Optional<UserFlashcardProgress> findByUserIdAndFlashcardId(Integer userId, Integer flashcardId);
}

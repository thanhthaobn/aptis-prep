package com.aptisprep.repository;

import com.aptisprep.entity.FlashcardSet;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlashcardSetRepository extends JpaRepository<FlashcardSet, Integer> {
    List<FlashcardSet> findByStatusOrderByIdDesc(String status);

    List<FlashcardSet> findByOwnerIsNullOrderByIdDesc();

    List<FlashcardSet> findByOwnerIdOrderByIdDesc(Integer ownerId);

    Optional<FlashcardSet> findFirstByLessonIdAndOwnerIsNullAndStatusOrderByIdDesc(
            Integer lessonId, String status);

    Optional<FlashcardSet> findFirstByLessonIdAndOwnerIsNullOrderByIdDesc(Integer lessonId);
}

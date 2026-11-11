package com.aptisprep.mvc.flashcard.repository;

import com.aptisprep.mvc.flashcard.entity.*;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlashcardRepository extends JpaRepository<Flashcard, Integer> {
    List<Flashcard> findBySetIdAndStatusOrderByDisplayOrderAsc(Integer setId, String status);
}

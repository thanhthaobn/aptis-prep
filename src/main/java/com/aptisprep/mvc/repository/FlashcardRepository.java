package com.aptisprep.mvc.repository;

import com.aptisprep.mvc.entity.Flashcard;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlashcardRepository extends JpaRepository<Flashcard, Integer> {
    List<Flashcard> findBySetIdAndStatusOrderByDisplayOrderAsc(Integer setId, String status);
}








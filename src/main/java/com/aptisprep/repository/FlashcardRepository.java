package com.aptisprep.repository;
import com.aptisprep.entity.Flashcard;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FlashcardRepository extends JpaRepository<Flashcard, Integer> {
    List<Flashcard> findBySetIdAndStatusOrderByDisplayOrderAsc(Integer setId,String status);
}

package com.aptisprep.mvc.flashcard.service;

import com.aptisprep.mvc.flashcard.dto.FlashcardSetFormDTO;
import com.aptisprep.mvc.flashcard.dto.FlashcardViewsDTO.*;
import com.aptisprep.mvc.flashcard.entity.FlashcardSet;
import com.aptisprep.mvc.flashcard.service.impl.FlashcardSetServiceImpl;
import com.aptisprep.mvc.learning.entity.Lesson;
import java.util.List;
import java.util.Map;

public interface FlashcardSetService {
    static List<SkillSets> groupBySkillAndLevel(List<SetView> values) {
        return FlashcardSetServiceImpl.groupBySkillAndLevel(values);
    }

    StudySetView studySet(Integer id, String email);

    List<SetView> list(boolean admin);

    List<SetView> listForStudent(String email);

    Map<Integer, SetView> lessonSets();

    FlashcardSet require(Integer id, boolean admin);

    FlashcardSet requireForStudent(Integer id, String email);

    FlashcardSetFormDTO form(Integer id);

    Integer saveAdmin(Integer id, FlashcardSetFormDTO form);

    FlashcardSetFormDTO personalForm(Integer id, String email);

    Integer savePersonal(Integer id, FlashcardSetFormDTO form, String email);

    void archivePersonal(Integer id, String email);

    List<CardView> study(Integer id, String email);

    FlashcardSet publishedForLesson(Integer lessonId);

    FlashcardSet forLesson(Integer lessonId);

    List<Lesson> availableLessons();

    void prepareForLesson(FlashcardSetFormDTO form, Integer lessonId);

    void mark(Integer setId, Integer cardId, String state, String email);

    void mark(Integer setId, Integer cardId, String state, String email, String reviewKey);

    List<java.time.LocalDateTime> findReviewedDatesByUserId(Integer userId);
}

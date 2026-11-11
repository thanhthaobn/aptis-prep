package com.aptisprep.service.learning;

import com.aptisprep.entity.FlashcardSet;
import com.aptisprep.entity.Lesson;
import com.aptisprep.presentation.FlashcardViewsDTO.*;
import com.aptisprep.presentation.form.FlashcardSetFormDTO;
import java.util.List;
import java.util.Map;

public interface FlashcardSetService {
    List<SkillSets> groupBySkillAndLevel(List<SetView> values);

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

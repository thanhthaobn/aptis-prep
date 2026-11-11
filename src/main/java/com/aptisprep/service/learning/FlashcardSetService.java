package com.aptisprep.service.learning;

import com.aptisprep.dto.FlashcardSetFormDTO;
import com.aptisprep.dto.FlashcardViewsDTO.*;
import com.aptisprep.entity.FlashcardSet;
import com.aptisprep.entity.Lesson;
import java.util.List;
import java.util.Map;

public interface FlashcardSetService {
    public List<SkillSets> groupBySkillAndLevel(List<SetView> values);

    public StudySetView studySet(Integer id, String email);

    public List<SetView> list(boolean admin);

    public List<SetView> listForStudent(String email);

    public Map<Integer, SetView> lessonSets();

    public FlashcardSet require(Integer id, boolean admin);

    public FlashcardSet requireForStudent(Integer id, String email);

    public FlashcardSetFormDTO form(Integer id);

    public Integer saveAdmin(Integer id, FlashcardSetFormDTO form);

    public FlashcardSetFormDTO personalForm(Integer id, String email);

    public Integer savePersonal(Integer id, FlashcardSetFormDTO form, String email);

    public void archivePersonal(Integer id, String email);

    public List<CardView> study(Integer id, String email);

    public FlashcardSet publishedForLesson(Integer lessonId);

    public FlashcardSet forLesson(Integer lessonId);

    public List<Lesson> availableLessons();

    public void prepareForLesson(FlashcardSetFormDTO form, Integer lessonId);

    public void mark(Integer setId, Integer cardId, String state, String email);

    public void mark(Integer setId, Integer cardId, String state, String email, String reviewKey);

    public List<java.time.LocalDateTime> findReviewedDatesByUserId(Integer userId);
}

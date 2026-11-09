package com.aptisprep.service.learning;

import com.aptisprep.dto.form.FlashcardSetForm;
import com.aptisprep.dto.view.FlashcardViews.*;
import com.aptisprep.entity.FlashcardSet;
import com.aptisprep.entity.Lesson;
import com.aptisprep.service.learning.impl.FlashcardSetServiceImpl;
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
    FlashcardSetForm form(Integer id);
    Integer saveAdmin(Integer id, FlashcardSetForm form);
    FlashcardSetForm personalForm(Integer id, String email);
    Integer savePersonal(Integer id, FlashcardSetForm form, String email);
    void archivePersonal(Integer id, String email);
    List<CardView> study(Integer id, String email);
    FlashcardSet publishedForLesson(Integer lessonId);
    FlashcardSet forLesson(Integer lessonId);
    List<Lesson> availableLessons();
    void prepareForLesson(FlashcardSetForm form, Integer lessonId);
    void mark(Integer setId, Integer cardId, String state, String email);
    void mark(Integer setId, Integer cardId, String state, String email, String reviewKey);
}

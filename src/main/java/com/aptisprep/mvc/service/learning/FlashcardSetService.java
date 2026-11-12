package com.aptisprep.mvc.service.learning;

import com.aptisprep.mvc.dto.FlashcardSetDTO;
import com.aptisprep.mvc.entity.FlashcardSet;
import com.aptisprep.mvc.entity.Lesson;
import java.util.List;
import java.util.Map;

public interface FlashcardSetService {
    public Map<String, List<FlashcardSetDTO>> groupBySkillAndLevel(List<FlashcardSetDTO> values);

    public FlashcardSetDTO studySet(Integer id, String email);

    public List<FlashcardSetDTO> list(boolean admin);

    public List<FlashcardSetDTO> listForStudent(String email);

    public List<FlashcardSetDTO> filteredListForStudent(
            String email, String source, String query, String skill, String level, String sort);

    public Map<Integer, FlashcardSetDTO> lessonSets();

    public FlashcardSet require(Integer id, boolean admin);
    public FlashcardSetDTO requireView(Integer id, boolean admin);

    public FlashcardSet requireForStudent(Integer id, String email);
    public FlashcardSetDTO requireForStudentView(Integer id, String email);

    public FlashcardSetDTO form(Integer id);

    public Integer saveAdmin(Integer id, FlashcardSetDTO form);

    public FlashcardSetDTO personalForm(Integer id, String email);

    public Integer savePersonal(Integer id, FlashcardSetDTO form, String email);

    public void archivePersonal(Integer id, String email);

    public List<com.aptisprep.mvc.dto.FlashcardDTO> study(Integer id, String email);

    public FlashcardSet publishedForLesson(Integer lessonId);
    public FlashcardSetDTO publishedForLessonView(Integer lessonId);

    public FlashcardSet forLesson(Integer lessonId);
    public FlashcardSetDTO forLessonView(Integer lessonId);

    public List<Lesson> availableLessons();
    public List<com.aptisprep.mvc.dto.LessonDTO> availableLessonViews();

    public void prepareForLesson(FlashcardSetDTO form, Integer lessonId);

    public void mark(Integer setId, Integer cardId, String state, String email);

    public void mark(Integer setId, Integer cardId, String state, String email, String reviewKey);
}








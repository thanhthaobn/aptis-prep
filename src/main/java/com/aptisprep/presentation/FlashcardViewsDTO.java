package com.aptisprep.presentation;

import java.time.LocalDateTime;
import java.util.List;

/** Presentation models shared by flashcard services, controllers and templates. */
public final class FlashcardViewsDTO {
    private FlashcardViewsDTO() {}

    public record SetView(
            Integer id,
            String title,
            String description,
            String level,
            String status,
            int count,
            boolean personal,
            Integer lessonId,
            String lessonTitle,
            String lessonSkill) {}

    public record LevelSets(String level, List<SetView> sets) {}

    public record SkillSets(String skill, int count, List<LevelSets> levels) {}

    public record CardView(
            Integer id, String term, String definition, String state, LocalDateTime nextReviewAt) {}

    public record LessonLink(Integer id, String title, String skill) {}

    public record StudySetView(
            Integer id, String title, String description, String cefrLevel, LessonLink lesson) {}
}

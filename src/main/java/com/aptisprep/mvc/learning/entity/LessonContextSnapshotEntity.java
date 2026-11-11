package com.aptisprep.mvc.learning.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "lesson_context_snapshots")
public class LessonContextSnapshotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String title;

    @Column(length = 30)
    private String skill;

    @Column(columnDefinition = "nvarchar(max)")
    private String description;

    @Column(columnDefinition = "nvarchar(max)")
    private String objectives;

    @Column(columnDefinition = "nvarchar(max)")
    private String strategy;

    @Column(name = "example_text", columnDefinition = "nvarchar(max)")
    private String exampleText;

    @Column(columnDefinition = "nvarchar(max)")
    private String analysis;

    @Column(name = "practice_prompt", columnDefinition = "nvarchar(max)")
    private String practicePrompt;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(name = "audio_path", length = 500)
    private String audioPath;

    @Column(name = "video_path", length = 500)
    private String videoPath;

    @Column(name = "video_url", length = 1000)
    private String videoUrl;

    @Column(name = "min_words")
    private Integer minWords;

    @Column(name = "max_words")
    private Integer maxWords;

    public static LessonContextSnapshotEntity capture(Lesson lesson) {
        LessonMaterial material = lesson.getMaterial();
        LessonContextSnapshotEntity value = new LessonContextSnapshotEntity();
        value.title = lesson.getTitle();
        value.skill = lesson.getSkill();
        value.description = lesson.getDescription();
        value.objectives = material == null ? null : material.getObjectives();
        value.strategy = material == null ? null : material.getStrategy();
        value.exampleText = material == null ? null : material.getExampleText();
        value.analysis = material == null ? null : material.getAnalysis();
        value.practicePrompt = material == null ? null : material.getPracticePrompt();
        value.imagePath = lesson.getImagePath();
        value.audioPath = lesson.getAudioPath();
        value.videoPath = lesson.getVideoPath();
        value.videoUrl = material == null ? null : material.getVideoUrl();
        value.minWords = lesson.getMinWordCount();
        value.maxWords = lesson.getMaxWordCount();
        return value;
    }

    public LessonContextSnapshot toRecord() {
        return new LessonContextSnapshot(
                title,
                skill,
                description,
                objectives,
                strategy,
                exampleText,
                analysis,
                practicePrompt,
                imagePath,
                audioPath,
                videoPath,
                videoUrl,
                minWords,
                maxWords);
    }
}

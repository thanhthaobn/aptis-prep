package com.aptisprep.mvc.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "lesson_context_snapshots")
public class LessonContextSnapshot {
    public LessonContextSnapshot() {}

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

    public LessonContextSnapshot(
            Integer id,
            String title,
            String skill,
            String description,
            String objectives,
            String strategy,
            String exampleText,
            String analysis,
            String practicePrompt,
            String imagePath,
            String audioPath,
            String videoPath,
            String videoUrl,
            Integer minWords,
            Integer maxWords) {
        this.id = id;
        this.title = title;
        this.skill = skill;
        this.description = description;
        this.objectives = objectives;
        this.strategy = strategy;
        this.exampleText = exampleText;
        this.analysis = analysis;
        this.practicePrompt = practicePrompt;
        this.imagePath = imagePath;
        this.audioPath = audioPath;
        this.videoPath = videoPath;
        this.videoUrl = videoUrl;
        this.minWords = minWords;
        this.maxWords = maxWords;
    }

    public static LessonContextSnapshot capture(Lesson lesson) {
        LessonContextSnapshot value = new LessonContextSnapshot();
        value.title = lesson.getTitle();
        value.skill = lesson.getSkill();
        value.description = lesson.getDescription();
        value.objectives = lesson.getObjectives();
        value.strategy = lesson.getStrategy();
        value.exampleText = lesson.getExampleText();
        value.analysis = lesson.getAnalysis();
        value.practicePrompt = lesson.getPracticePrompt();
        value.imagePath = lesson.getImagePath();
        value.audioPath = lesson.getAudioPath();
        value.videoPath = lesson.getVideoPath();
        value.videoUrl = lesson.getVideoUrl();
        value.minWords = lesson.getMinWordCount();
        value.maxWords = lesson.getMaxWordCount();
        return value;
    }

    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSkill() {
        return skill;
    }

    public String getDescription() {
        return description;
    }

    public String getObjectives() {
        return objectives;
    }

    public String getStrategy() {
        return strategy;
    }

    public String getExampleText() {
        return exampleText;
    }

    public String getAnalysis() {
        return analysis;
    }

    public String getPracticePrompt() {
        return practicePrompt;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setId(Integer value) {
        this.id = value;
    }

    public void setTitle(String value) {
        this.title = value;
    }

    public void setSkill(String value) {
        this.skill = value;
    }

    public void setDescription(String value) {
        this.description = value;
    }

    public void setObjectives(String value) {
        this.objectives = value;
    }

    public void setStrategy(String value) {
        this.strategy = value;
    }

    public void setExampleText(String value) {
        this.exampleText = value;
    }

    public void setAnalysis(String value) {
        this.analysis = value;
    }

    public void setPracticePrompt(String value) {
        this.practicePrompt = value;
    }

    public void setImagePath(String value) {
        this.imagePath = value;
    }

    public void setAudioPath(String value) {
        this.audioPath = value;
    }

    public void setVideoPath(String value) {
        this.videoPath = value;
    }

    public void setVideoUrl(String value) {
        this.videoUrl = value;
    }

    public Integer getMinWords() {
        return minWords;
    }

    public void setMinWords(Integer value) {
        this.minWords = value;
    }

    public Integer getMaxWords() {
        return maxWords;
    }

    public void setMaxWords(Integer value) {
        this.maxWords = value;
    }
}

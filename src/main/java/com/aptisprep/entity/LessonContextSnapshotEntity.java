package com.aptisprep.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "lesson_context_snapshots")
public class LessonContextSnapshotEntity {
    public LessonContextSnapshotEntity() {}

    public LessonContextSnapshotEntity(
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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setObjectives(String objectives) {
        this.objectives = objectives;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public void setExampleText(String exampleText) {
        this.exampleText = exampleText;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }

    public void setPracticePrompt(String practicePrompt) {
        this.practicePrompt = practicePrompt;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public Integer getMinWords() {
        return minWords;
    }

    public void setMinWords(Integer minWords) {
        this.minWords = minWords;
    }

    public Integer getMaxWords() {
        return maxWords;
    }

    public void setMaxWords(Integer maxWords) {
        this.maxWords = maxWords;
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
}

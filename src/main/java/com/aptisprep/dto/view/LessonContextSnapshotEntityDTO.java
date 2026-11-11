package com.aptisprep.dto.view;

import com.aptisprep.entity.Lesson;
import com.aptisprep.entity.LessonContextSnapshotEntity;
import com.aptisprep.entity.LessonMaterial;

/** Stimulus data exposed to the learning views. */
public class LessonContextSnapshotEntityDTO {
    private String title;
    private String skill;
    private String description;
    private String objectives;
    private String strategy;
    private String exampleText;
    private String analysis;
    private String practicePrompt;
    private String imagePath;
    private String audioPath;
    private String videoPath;
    private String videoUrl;
    private Integer minWords;
    private Integer maxWords;

    public LessonContextSnapshotEntityDTO() {}

    public LessonContextSnapshotEntityDTO(
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

    public static LessonContextSnapshotEntityDTO convertEntityToDto(Lesson lesson) {
        LessonMaterial material = lesson.getMaterial();
        return new LessonContextSnapshotEntityDTO(
                lesson.getTitle(),
                lesson.getSkill(),
                lesson.getDescription(),
                material == null ? null : material.getObjectives(),
                material == null ? null : material.getStrategy(),
                material == null ? null : material.getExampleText(),
                material == null ? null : material.getAnalysis(),
                material == null ? null : material.getPracticePrompt(),
                lesson.getImagePath(),
                lesson.getAudioPath(),
                lesson.getVideoPath(),
                material == null ? null : material.getVideoUrl(),
                lesson.getMinWordCount(),
                lesson.getMaxWordCount());
    }

    public static LessonContextSnapshotEntityDTO convertEntityToDto(
            LessonContextSnapshotEntity entity) {
        if (entity == null) return null;
        return new LessonContextSnapshotEntityDTO(
                entity.getTitle(),
                entity.getSkill(),
                entity.getDescription(),
                entity.getObjectives(),
                entity.getStrategy(),
                entity.getExampleText(),
                entity.getAnalysis(),
                entity.getPracticePrompt(),
                entity.getImagePath(),
                entity.getAudioPath(),
                entity.getVideoPath(),
                entity.getVideoUrl(),
                entity.getMinWords(),
                entity.getMaxWords());
    }

    public static LessonContextSnapshotEntity convertDtoToEntity(
            LessonContextSnapshotEntityDTO dto) {
        if (dto == null) return null;
        return new LessonContextSnapshotEntity(
                null,
                dto.title,
                dto.skill,
                dto.description,
                dto.objectives,
                dto.strategy,
                dto.exampleText,
                dto.analysis,
                dto.practicePrompt,
                dto.imagePath,
                dto.audioPath,
                dto.videoPath,
                dto.videoUrl,
                dto.minWords,
                dto.maxWords);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getObjectives() {
        return objectives;
    }

    public void setObjectives(String objectives) {
        this.objectives = objectives;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getExampleText() {
        return exampleText;
    }

    public void setExampleText(String exampleText) {
        this.exampleText = exampleText;
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }

    public String getPracticePrompt() {
        return practicePrompt;
    }

    public void setPracticePrompt(String practicePrompt) {
        this.practicePrompt = practicePrompt;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public String getVideoUrl() {
        return videoUrl;
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
}

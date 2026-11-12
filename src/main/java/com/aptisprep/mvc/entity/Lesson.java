package com.aptisprep.mvc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lessons")
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "transcript_visibility", length = 30)
    private String transcriptVisibility;

    @Column(name = "sample_visibility", length = 30)
    private String sampleVisibility;

    @Column(length = 160)
    private String topic;

    @Column(name = "task_type", length = 160)
    private String taskType;

    @Column(columnDefinition = "nvarchar(max)")
    private String objectives;

    @Column(columnDefinition = "nvarchar(max)")
    private String strategy;

    @Column(name = "example_text", columnDefinition = "nvarchar(max)")
    private String exampleText;

    @Column(columnDefinition = "nvarchar(max)")
    private String analysis;

    @Column(name = "common_mistakes", columnDefinition = "nvarchar(max)")
    private String commonMistakes;

    @Column(name = "practice_prompt", columnDefinition = "nvarchar(max)")
    private String practicePrompt;

    @Column(name = "sample_response", columnDefinition = "nvarchar(max)")
    private String sampleResponse;

    @Column(columnDefinition = "nvarchar(max)")
    private String checklist;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "source_name", length = 255)
    private String sourceName;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "nvarchar(max)")
    private String description;

    @Column(nullable = false, length = 30)
    private String skill;

    @Column(name = "cefr_level", nullable = false, length = 10)
    private String cefrLevel;

    @Column(name = "video_path", length = 500)
    private String videoPath;

    @Column(name = "audio_path", length = 500)
    private String audioPath;

    @Column(name = "audio_name", length = 255)
    private String audioName;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(name = "image_name", length = 255)
    private String imageName;

    @Column(name = "transcript", columnDefinition = "nvarchar(max)")
    private String transcript;

    @Column(name = "min_word_count")
    private Integer minWordCount;

    @Column(name = "max_word_count")
    private Integer maxWordCount;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Lesson() {}

    public Lesson(
            Integer id,
            String title,
            String description,
            String skill,
            String cefrLevel,
            String videoPath,
            String audioPath,
            String audioName,
            String imagePath,
            String imageName,
            String transcript,
            Integer minWordCount,
            Integer maxWordCount,
            Integer displayOrder,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String transcriptVisibility,
            String sampleVisibility,
            String topic,
            String taskType,
            String objectives,
            String strategy,
            String exampleText,
            String analysis,
            String commonMistakes,
            String practicePrompt,
            String sampleResponse,
            String checklist,
            String videoUrl,
            String sourceUrl,
            String sourceName) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.skill = skill;
        this.cefrLevel = cefrLevel;
        this.videoPath = videoPath;
        this.audioPath = audioPath;
        this.audioName = audioName;
        this.imagePath = imagePath;
        this.imageName = imageName;
        this.transcript = transcript;
        this.minWordCount = minWordCount;
        this.maxWordCount = maxWordCount;
        this.displayOrder = displayOrder;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.transcriptVisibility = transcriptVisibility;
        this.sampleVisibility = sampleVisibility;
        this.topic = topic;
        this.taskType = taskType;
        this.objectives = objectives;
        this.strategy = strategy;
        this.exampleText = exampleText;
        this.analysis = analysis;
        this.commonMistakes = commonMistakes;
        this.practicePrompt = practicePrompt;
        this.sampleResponse = sampleResponse;
        this.checklist = checklist;
        this.videoUrl = videoUrl;
        this.sourceUrl = sourceUrl;
        this.sourceName = sourceName;
    }

    @PrePersist
    void create() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void update() {
        updatedAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTranscriptVisibility() { return transcriptVisibility; }
    public void setTranscriptVisibility(String value) { transcriptVisibility = trim(value); }
    public String getSampleVisibility() { return sampleVisibility; }
    public void setSampleVisibility(String value) { sampleVisibility = trim(value); }
    public String getTopic() { return topic; }
    public void setTopic(String value) { topic = trim(value); }
    public String getTaskType() { return taskType; }
    public void setTaskType(String value) { taskType = trim(value); }
    public String getObjectives() { return objectives; }
    public void setObjectives(String value) { objectives = trim(value); }
    public String getStrategy() { return strategy; }
    public void setStrategy(String value) { strategy = trim(value); }
    public String getExampleText() { return exampleText; }
    public void setExampleText(String value) { exampleText = trim(value); }
    public String getAnalysis() { return analysis; }
    public void setAnalysis(String value) { analysis = trim(value); }
    public String getCommonMistakes() { return commonMistakes; }
    public void setCommonMistakes(String value) { commonMistakes = trim(value); }
    public String getPracticePrompt() { return practicePrompt; }
    public void setPracticePrompt(String value) { practicePrompt = trim(value); }
    public String getSampleResponse() { return sampleResponse; }
    public void setSampleResponse(String value) { sampleResponse = trim(value); }
    public String getChecklist() { return checklist; }
    public void setChecklist(String value) { checklist = trim(value); }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String value) { videoUrl = trim(value); }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String value) { sourceUrl = trim(value); }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String value) { sourceName = trim(value); }

    public String getTitle() {
        return title;
    }

    public void setTitle(String v) {
        title = v;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String v) {
        description = v;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String v) {
        skill = v;
    }

    public String getCefrLevel() {
        return cefrLevel;
    }

    public void setCefrLevel(String v) {
        cefrLevel = v;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String v) {
        videoPath = v;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String v) {
        audioPath = v;
    }

    public String getAudioName() {
        return audioName;
    }

    public void setAudioName(String v) {
        audioName = v;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String v) {
        imagePath = v;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String v) {
        imageName = v;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String v) {
        transcript = v;
    }

    public Integer getMinWordCount() {
        return minWordCount;
    }

    public void setMinWordCount(Integer v) {
        minWordCount = v;
    }

    public Integer getMaxWordCount() {
        return maxWordCount;
    }

    public void setMaxWordCount(Integer v) {
        maxWordCount = v;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer v) {
        displayOrder = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime value) {
        createdAt = value;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime value) {
        updatedAt = value;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}








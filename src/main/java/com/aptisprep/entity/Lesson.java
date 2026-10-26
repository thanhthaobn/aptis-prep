package com.aptisprep.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lessons")
public class Lesson {
    @Embedded private LessonMaterial material = new LessonMaterial();

    public LessonMaterial getMaterial() {
        return material;
    }

    public void setMaterial(LessonMaterial value) {
        material = value;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

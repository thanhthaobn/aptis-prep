package com.aptisprep.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "question_media")
public class QuestionMediaEntity {
    public QuestionMediaEntity(
            Integer id,
            Integer questionId,
            String mediaType,
            String mediaUrl,
            String originalFilename,
            String contentType,
            Long fileSize,
            Integer maxPlayCount,
            int position,
            String altText,
            String transcript,
            LocalDateTime createdAt) {
        this.id = id;
        this.questionId = questionId;
        this.mediaType = mediaType;
        this.mediaUrl = mediaUrl;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.maxPlayCount = maxPlayCount;
        this.position = position;
        this.altText = altText;
        this.transcript = transcript;
        this.createdAt = createdAt;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "question_id", nullable = false)
    private Integer questionId;

    @Column(name = "media_type", nullable = false, length = 20)
    private String mediaType;

    @Column(name = "media_url", nullable = false, length = 500)
    private String mediaUrl;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "max_play_count")
    private Integer maxPlayCount;

    @Column(nullable = false)
    private int position;

    @Column(name = "alt_text", length = 500)
    private String altText;

    @Column(columnDefinition = "nvarchar(max)")
    private String transcript;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public QuestionMediaEntity() {}

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public Integer getMaxPlayCount() {
        return maxPlayCount;
    }

    public String getContentType() {
        return contentType;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public int getPosition() {
        return position;
    }

    public String getAltText() {
        return altText;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setMaxPlayCount(Integer v) {
        maxPlayCount = v;
    }

    public void setQuestionId(Integer v) {
        questionId = v;
    }

    public void setMediaType(String v) {
        mediaType = v;
    }

    public void setMediaUrl(String v) {
        mediaUrl = v;
    }

    public void setOriginalFilename(String v) {
        originalFilename = v;
    }

    public void setContentType(String v) {
        contentType = v;
    }

    public void setFileSize(Long v) {
        fileSize = v;
    }

    public void setPosition(int v) {
        position = v;
    }

    public void setAltText(String v) {
        altText = v;
    }

    public void setTranscript(String v) {
        transcript = v;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

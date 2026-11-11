package com.aptisprep.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "exam_attempt_snapshot_media")
public class ExamMediaSnapshotEntity {
    public ExamMediaSnapshotEntity() {}

    public ExamMediaSnapshotEntity(
            ExamQuestionSnapshotEntity question,
            Integer sourceMediaId,
            Integer position,
            String mediaType,
            String mediaUrl,
            String originalFilename,
            String contentType,
            String transcript,
            Integer maxPlayCount) {
        this.question = question;
        this.sourceMediaId = sourceMediaId;
        this.position = position;
        this.mediaType = mediaType;
        this.mediaUrl = mediaUrl;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.transcript = transcript;
        this.maxPlayCount = maxPlayCount;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_snapshot_id", nullable = false)
    private ExamQuestionSnapshotEntity question;

    @Column(name = "source_media_id", nullable = false)
    private Integer sourceMediaId;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "media_type", length = 30)
    private String mediaType;

    @Column(name = "media_url", columnDefinition = "nvarchar(max)")
    private String mediaUrl;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(columnDefinition = "nvarchar(max)")
    private String transcript;

    @Column(name = "max_play_count")
    private Integer maxPlayCount;

    public Integer getId() {
        return id;
    }

    public ExamQuestionSnapshotEntity getQuestion() {
        return question;
    }

    public void setQuestion(ExamQuestionSnapshotEntity value) {
        question = value;
    }

    public Integer getSourceMediaId() {
        return sourceMediaId;
    }

    public void setSourceMediaId(Integer value) {
        sourceMediaId = value;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer value) {
        position = value;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String value) {
        mediaType = value;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String value) {
        mediaUrl = value;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String value) {
        originalFilename = value;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String value) {
        contentType = value;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String value) {
        transcript = value;
    }

    public Integer getMaxPlayCount() {
        return maxPlayCount;
    }

    public void setMaxPlayCount(Integer value) {
        maxPlayCount = value;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}

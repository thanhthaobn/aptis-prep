package com.aptisprep.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "question_media")
public class QuestionMediaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "question_id", nullable = false) private Integer questionId;
    @Column(name = "media_type", nullable = false, length = 20) private String mediaType;
    @Column(name = "media_url", nullable = false, length = 500) private String mediaUrl;
    @Column(name = "original_filename", length = 255) private String originalFilename;
    @Column(name = "content_type", length = 100) private String contentType;
    @Column(name = "file_size") private Long fileSize;
    @Column(name = "max_play_count") private Integer maxPlayCount;
    @Column(nullable = false) private int position;
    @Column(name = "alt_text", length = 500) private String altText;
    @Column(columnDefinition = "nvarchar(max)") private String transcript;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    public QuestionMediaEntity() {}
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
    public Integer getId() { return id; }
    public String getMediaType() { return mediaType; }
    public String getMediaUrl() { return mediaUrl; }
    public void setQuestionId(Integer v) { questionId = v; }
    public void setMediaType(String v) { mediaType = v; }
    public void setMediaUrl(String v) { mediaUrl = v; }
    public void setOriginalFilename(String v) { originalFilename = v; }
    public void setContentType(String v) { contentType = v; }
    public void setFileSize(Long v) { fileSize = v; }
    public void setPosition(int v) { position = v; }
    public void setAltText(String v) { altText = v; }
    public void setTranscript(String v) { transcript = v; }
}

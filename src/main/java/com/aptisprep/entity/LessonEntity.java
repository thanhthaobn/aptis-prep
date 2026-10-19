package com.aptisprep.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "lessons")
public class LessonEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(nullable = false, length = 255) private String title;
    @Column(columnDefinition = "nvarchar(max)") private String description;
    @Column(nullable = false, length = 30) private String skill;
    @Column(name = "cefr_level", nullable = false, length = 10) private String cefrLevel;
    @Column(name = "video_path", length = 500) private String videoPath;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    public Integer getId() { return id; }
    public String getTitle() { return title; }
    public String getSkill() { return skill; }
    public String getCefrLevel() { return cefrLevel; }
    public int getDisplayOrder() { return displayOrder; }
    public String getStatus() { return status; }
}

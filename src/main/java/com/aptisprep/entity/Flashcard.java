package com.aptisprep.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "flashcards")
public class Flashcard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "set_id", nullable = false)
    private FlashcardSet set;

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    public FlashcardSet getSet() {
        return set;
    }

    public void setSet(FlashcardSet v) {
        set = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    @Column(name = "front_content", nullable = false, columnDefinition = "nvarchar(max)")
    private String frontContent;

    @Column(name = "back_content", nullable = false, columnDefinition = "nvarchar(max)")
    private String backContent;

    @Column(name = "cefr_level", length = 10)
    private String cefrLevel;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void create() {
        var now = LocalDateTime.now();
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

    public String getFrontContent() {
        return frontContent;
    }

    public void setFrontContent(String v) {
        frontContent = v;
    }

    public String getBackContent() {
        return backContent;
    }

    public void setBackContent(String v) {
        backContent = v;
    }

    public String getCefrLevel() {
        return cefrLevel;
    }

    public void setCefrLevel(String v) {
        cefrLevel = v;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer v) {
        displayOrder = v;
    }
}

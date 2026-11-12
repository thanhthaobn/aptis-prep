package com.aptisprep.mvc.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "flashcard_sets")
public class FlashcardSet {
    public FlashcardSet() {}

    public FlashcardSet(
            Integer id,
            String title,
            String description,
            String cefrLevel,
            String status,
            Lesson lesson,
            User owner,
            String visibility) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.cefrLevel = cefrLevel;
        this.status = status;
        this.lesson = lesson;
        this.owner = owner;
        this.visibility = visibility;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "cefr_level", nullable = false, length = 10)
    private String cefrLevel;

    @Column(nullable = false, length = 30)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    @Column(nullable = false, length = 20)
    private String visibility = "PUBLIC";

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

    public String getCefrLevel() {
        return cefrLevel;
    }

    public void setCefrLevel(String v) {
        cefrLevel = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public void setLesson(Lesson v) {
        lesson = v;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User v) {
        owner = v;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String v) {
        visibility = v;
    }

    public void setId(Integer value) {
        this.id = value;
    }
}

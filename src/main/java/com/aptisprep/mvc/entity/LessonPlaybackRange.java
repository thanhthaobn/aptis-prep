package com.aptisprep.mvc.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "lesson_playback_ranges")
public class LessonPlaybackRange {
    public LessonPlaybackRange() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "engagement_id", nullable = false)
    private LessonEngagement engagement;

    @Column(name = "start_seconds", nullable = false)
    private double startSeconds;

    @Column(name = "end_seconds", nullable = false)
    private double endSeconds;

    public LessonPlaybackRange(
            Integer id, LessonEngagement engagement, double startSeconds, double endSeconds) {
        this.id = id;
        this.engagement = engagement;
        this.startSeconds = startSeconds;
        this.endSeconds = endSeconds;
    }

    public Integer getId() {
        return id;
    }

    public LessonEngagement getEngagement() {
        return engagement;
    }

    public void setEngagement(LessonEngagement value) {
        engagement = value;
    }

    public double getStartSeconds() {
        return startSeconds;
    }

    public void setStartSeconds(double value) {
        startSeconds = value;
    }

    public double getEndSeconds() {
        return endSeconds;
    }

    public void setEndSeconds(double value) {
        endSeconds = value;
    }

    public void setId(Integer value) {
        this.id = value;
    }
}

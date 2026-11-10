package com.aptisprep.mvc.exam.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exam_attempt_snapshot_questions")
public class ExamQuestionSnapshotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private ExamAttemptSnapshotEntity snapshot;

    @Column(name = "source_question_id", nullable = false)
    private Integer sourceQuestionId;

    @Column(nullable = false)
    private Integer position;

    @Column(length = 30)
    private String skill;

    @Column(name = "question_type", length = 50)
    private String questionType;

    @Column(columnDefinition = "nvarchar(max)")
    private String content;

    @Column(columnDefinition = "nvarchar(max)")
    private String instruction;

    @Column(name = "reading_passage", columnDefinition = "nvarchar(max)")
    private String readingPassage;

    @Column(name = "section_start_minute")
    private Integer sectionStartMinute;

    @Column(name = "section_end_minute")
    private Integer sectionEndMinute;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<ExamItemSnapshotEntity> items = new ArrayList<>();

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<ExamMediaSnapshotEntity> media = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public ExamAttemptSnapshotEntity getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(ExamAttemptSnapshotEntity value) {
        snapshot = value;
    }

    public Integer getSourceQuestionId() {
        return sourceQuestionId;
    }

    public void setSourceQuestionId(Integer value) {
        sourceQuestionId = value;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer value) {
        position = value;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String value) {
        skill = value;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String value) {
        questionType = value;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String value) {
        content = value;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String value) {
        instruction = value;
    }

    public String getReadingPassage() {
        return readingPassage;
    }

    public void setReadingPassage(String value) {
        readingPassage = value;
    }

    public Integer getSectionStartMinute() {
        return sectionStartMinute;
    }

    public void setSectionStartMinute(Integer value) {
        sectionStartMinute = value;
    }

    public Integer getSectionEndMinute() {
        return sectionEndMinute;
    }

    public void setSectionEndMinute(Integer value) {
        sectionEndMinute = value;
    }

    public List<ExamItemSnapshotEntity> getItems() {
        return items;
    }

    public List<ExamMediaSnapshotEntity> getMedia() {
        return media;
    }

    public void addItem(ExamItemSnapshotEntity item) {
        items.add(item);
        item.setQuestion(this);
    }

    public void addMedia(ExamMediaSnapshotEntity value) {
        media.add(value);
        value.setQuestion(this);
    }
}

package com.aptisprep.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exam_attempt_snapshot_items")
public class ExamItemSnapshotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_snapshot_id", nullable = false)
    private ExamQuestionSnapshotEntity question;

    @Column(name = "source_item_id", nullable = false)
    private Integer sourceItemId;

    @Column(nullable = false)
    private Integer position;

    @Column(columnDefinition = "nvarchar(max)")
    private String content;

    @Column(name = "correct_position")
    private Integer correctPosition;

    @Column(name = "min_word_count")
    private Integer minWordCount;

    @Column(name = "max_word_count")
    private Integer maxWordCount;

    @Column(name = "response_seconds")
    private Integer responseSeconds;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<ExamOptionSnapshotEntity> options = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public ExamQuestionSnapshotEntity getQuestion() {
        return question;
    }

    public void setQuestion(ExamQuestionSnapshotEntity value) {
        question = value;
    }

    public Integer getSourceItemId() {
        return sourceItemId;
    }

    public void setSourceItemId(Integer value) {
        sourceItemId = value;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer value) {
        position = value;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String value) {
        content = value;
    }

    public Integer getCorrectPosition() {
        return correctPosition;
    }

    public void setCorrectPosition(Integer value) {
        correctPosition = value;
    }

    public Integer getMinWordCount() {
        return minWordCount;
    }

    public void setMinWordCount(Integer value) {
        minWordCount = value;
    }

    public Integer getMaxWordCount() {
        return maxWordCount;
    }

    public void setMaxWordCount(Integer value) {
        maxWordCount = value;
    }

    public Integer getResponseSeconds() {
        return responseSeconds;
    }

    public void setResponseSeconds(Integer value) {
        responseSeconds = value;
    }

    public List<ExamOptionSnapshotEntity> getOptions() {
        return options;
    }

    public void addOption(ExamOptionSnapshotEntity value) {
        options.add(value);
        value.setItem(this);
    }
}

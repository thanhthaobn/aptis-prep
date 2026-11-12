package com.aptisprep.mvc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exam_attempt_snapshot_items")
public class ExamItemSnapshot {
    public ExamItemSnapshot() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_snapshot_id", nullable = false)
    private ExamQuestionSnapshot question;

    @Column(name = "source_item_id", nullable = false)
    private Integer sourceItemId;

    @Column(nullable = false)
    private Integer position;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal points = BigDecimal.ONE;

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
    private List<ExamOptionSnapshot> options = new ArrayList<>();

    public ExamItemSnapshot(
            Integer id,
            ExamQuestionSnapshot question,
            Integer sourceItemId,
            Integer position,
            BigDecimal points,
            String content,
            Integer correctPosition,
            Integer minWordCount,
            Integer maxWordCount,
            Integer responseSeconds,
            List<ExamOptionSnapshot> options) {
        this.id = id;
        this.question = question;
        this.sourceItemId = sourceItemId;
        this.position = position;
        this.points = points == null ? BigDecimal.ONE : points;
        this.content = content;
        this.correctPosition = correctPosition;
        this.minWordCount = minWordCount;
        this.maxWordCount = maxWordCount;
        this.responseSeconds = responseSeconds;
        this.options = options;
    }


    public Integer getId() {
        return id;
    }

    public ExamQuestionSnapshot getQuestion() {
        return question;
    }

    public void setQuestion(ExamQuestionSnapshot value) {
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

    public BigDecimal getPoints() {
        return points;
    }

    public void setPoints(BigDecimal value) {
        points = value == null ? BigDecimal.ONE : value;
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

    public List<ExamOptionSnapshot> getOptions() {
        return options;
    }

    public void addOption(ExamOptionSnapshot value) {
        options.add(value);
        value.setItem(this);
    }


    public void setId(Integer value) {
        this.id = value;
    }

    public void setOptions(List<ExamOptionSnapshot> value) {
        this.options = value;
    }
}








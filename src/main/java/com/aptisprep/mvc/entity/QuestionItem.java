package com.aptisprep.mvc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "question_items")
public class QuestionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "question_id", nullable = false)
    private Integer questionId;

    @Column(nullable = false, columnDefinition = "nvarchar(max)")
    private String content;

    @Column(name = "correct_position")
    private Integer correctPosition;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, precision = 6, scale = 2, columnDefinition = "decimal(6,2) default 1")
    private BigDecimal points = BigDecimal.ONE;

    @Column(name = "min_word_count")
    private Integer minWordCount;

    @Column(name = "max_word_count")
    private Integer maxWordCount;

    @Column(name = "response_seconds")
    private Integer responseSeconds;

    public QuestionItem(
            Integer id,
            Integer questionId,
            String content,
            Integer correctPosition,
            int position,
            BigDecimal points,
            Integer minWordCount,
            Integer maxWordCount,
            Integer responseSeconds) {
        this.id = id;
        this.questionId = questionId;
        this.content = content;
        this.correctPosition = correctPosition;
        this.position = position;
        this.points = points == null ? BigDecimal.ONE : points;
        this.minWordCount = minWordCount;
        this.maxWordCount = maxWordCount;
        this.responseSeconds = responseSeconds;
    }

    public QuestionItem() {}

    public Integer getId() {
        return id;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer v) {
        questionId = v;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String v) {
        content = v;
    }

    public Integer getCorrectPosition() {
        return correctPosition;
    }

    public void setCorrectPosition(Integer v) {
        correctPosition = v;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int v) {
        position = v;
    }

    public BigDecimal getPoints() {
        return points;
    }

    public void setPoints(BigDecimal v) {
        points = v == null ? BigDecimal.ONE : v;
    }

    public Integer getMinWordCount() {
        return minWordCount;
    }

    public void setMinWordCount(Integer v) {
        minWordCount = v;
    }

    public Integer getMaxWordCount() {
        return maxWordCount;
    }

    public void setMaxWordCount(Integer v) {
        maxWordCount = v;
    }

    public Integer getResponseSeconds() {
        return responseSeconds;
    }

    public void setResponseSeconds(Integer v) {
        responseSeconds = v;
    }

    public void setId(Integer value) {
        this.id = value;
    }
}

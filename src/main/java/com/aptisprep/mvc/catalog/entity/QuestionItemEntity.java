package com.aptisprep.mvc.catalog.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "question_items")
public class QuestionItemEntity {
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

    @Column(name = "min_word_count")
    private Integer minWordCount;

    @Column(name = "max_word_count")
    private Integer maxWordCount;

    @Column(name = "response_seconds")
    private Integer responseSeconds;

    public QuestionItemEntity() {}

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
}

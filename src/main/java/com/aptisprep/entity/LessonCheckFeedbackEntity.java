package com.aptisprep.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "lesson_check_attempt_feedback")
public class LessonCheckFeedbackEntity {
    public LessonCheckFeedbackEntity() {}

    public LessonCheckFeedbackEntity(
            LessonCheckAttempt attempt,
            String prompt,
            String selected,
            String correct,
            boolean rightAnswer,
            String explanation,
            String optionsText) {
        this.attempt = attempt;
        this.prompt = prompt;
        this.selected = selected;
        this.correct = correct;
        this.rightAnswer = rightAnswer;
        this.explanation = explanation;
        this.optionsText = optionsText;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private LessonCheckAttempt attempt;

    @Column(nullable = false, columnDefinition = "nvarchar(max)")
    private String prompt;

    @Column(columnDefinition = "nvarchar(max)")
    private String selected;

    @Column(columnDefinition = "nvarchar(max)")
    private String correct;

    @Column(nullable = false)
    private boolean rightAnswer;

    @Column(columnDefinition = "nvarchar(max)")
    private String explanation;

    @Column(name = "options_text", columnDefinition = "nvarchar(max)")
    private String optionsText;

    public Integer getId() {
        return id;
    }

    public LessonCheckAttempt getAttempt() {
        return attempt;
    }

    public void setAttempt(LessonCheckAttempt value) {
        attempt = value;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String value) {
        prompt = value;
    }

    public String getSelected() {
        return selected;
    }

    public void setSelected(String value) {
        selected = value;
    }

    public String getCorrect() {
        return correct;
    }

    public void setCorrect(String value) {
        correct = value;
    }

    public boolean isRightAnswer() {
        return rightAnswer;
    }

    public void setRightAnswer(boolean value) {
        rightAnswer = value;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String value) {
        explanation = value;
    }

    public String getOptionsText() {
        return optionsText;
    }

    public void setOptionsText(String value) {
        optionsText = value;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}

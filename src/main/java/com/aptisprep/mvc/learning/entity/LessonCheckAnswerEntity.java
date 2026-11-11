package com.aptisprep.mvc.learning.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "lesson_check_attempt_answers")
public class LessonCheckAnswerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private LessonCheckAttempt attempt;

    @Column(name = "question_id", nullable = false)
    private Integer questionId;

    @Column(name = "selected_option", nullable = false)
    private Integer selectedOption;

    public Integer getId() {
        return id;
    }

    public LessonCheckAttempt getAttempt() {
        return attempt;
    }

    public void setAttempt(LessonCheckAttempt value) {
        attempt = value;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer value) {
        questionId = value;
    }

    public Integer getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(Integer value) {
        selectedOption = value;
    }
}

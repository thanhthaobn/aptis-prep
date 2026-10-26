package com.aptisprep.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "lesson_checks")
public class LessonCheck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "lesson_id", nullable = false)
    private Integer lessonId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 2000)
    private String prompt;

    @Column(name = "option_a", nullable = false, length = 1000)
    private String optionA;

    @Column(name = "option_b", nullable = false, length = 1000)
    private String optionB;

    @Column(name = "option_c", length = 1000)
    private String optionC;

    @Column(name = "option_d", length = 1000)
    private String optionD;

    @Column(name = "option_e", length = 1000)
    private String optionE;

    @Column(name = "option_f", length = 1000)
    private String optionF;

    @Column(name = "correct_option", nullable = false)
    private int correctOption;

    @Column(nullable = false, length = 4000)
    private String explanation;

    public Integer getId() {
        return id;
    }

    public Integer getLessonId() {
        return lessonId;
    }

    public void setLessonId(Integer v) {
        lessonId = v;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int v) {
        position = v;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String v) {
        prompt = v;
    }

    public String getOptionA() {
        return optionA;
    }

    public void setOptionA(String v) {
        optionA = v;
    }

    public String getOptionB() {
        return optionB;
    }

    public void setOptionB(String v) {
        optionB = v;
    }

    public String getOptionC() {
        return optionC;
    }

    public void setOptionC(String v) {
        optionC = v;
    }

    public String getOptionD() {
        return optionD;
    }

    public void setOptionD(String v) {
        optionD = v;
    }

    public String getOptionE() {
        return optionE;
    }

    public void setOptionE(String v) {
        optionE = v;
    }

    public String getOptionF() {
        return optionF;
    }

    public void setOptionF(String v) {
        optionF = v;
    }

    public int getCorrectOption() {
        return correctOption;
    }

    public void setCorrectOption(int v) {
        correctOption = v;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String v) {
        explanation = v;
    }
}

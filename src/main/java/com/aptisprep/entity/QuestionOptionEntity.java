package com.aptisprep.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "question_options")
public class QuestionOptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "question_item_id", nullable = false)
    private Integer questionItemId;

    @Column(nullable = false, columnDefinition = "nvarchar(max)")
    private String content;

    @Column(name = "option_code", length = 50)
    private String optionCode;

    @Column(name = "is_correct")
    private Boolean correct;

    @Column(nullable = false)
    private int position;

    public QuestionOptionEntity() {}

    public Integer getId() {
        return id;
    }

    public Integer getQuestionItemId() {
        return questionItemId;
    }

    public void setQuestionItemId(Integer v) {
        questionItemId = v;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String v) {
        content = v;
    }

    public String getOptionCode() {
        return optionCode;
    }

    public void setOptionCode(String v) {
        optionCode = v;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean v) {
        correct = v;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int v) {
        position = v;
    }
}

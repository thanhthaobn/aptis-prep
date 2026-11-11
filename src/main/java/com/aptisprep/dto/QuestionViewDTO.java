package com.aptisprep.dto;

public class QuestionViewDTO {
    private Integer questionId;
    private String skill;
    private String type;
    private int position;
    private String content;

    public QuestionViewDTO() {}

    public QuestionViewDTO(
            Integer questionId, String skill, String type, int position, String content) {
        this.questionId = questionId;
        this.skill = skill;
        this.type = type;
        this.position = position;
        this.content = content;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public Integer questionId() {
        return questionId;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public String skill() {
        return skill;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String type() {
        return type;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int position() {
        return position;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String content() {
        return content;
    }
}

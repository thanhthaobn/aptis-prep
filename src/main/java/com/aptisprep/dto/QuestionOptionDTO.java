package com.aptisprep.dto;

import com.aptisprep.entity.QuestionOptionEntity;

public class QuestionOptionDTO {
    private Integer id;
    private Integer questionItemId;
    private String content;
    private String optionCode;
    private Boolean correct = false;
    private Integer position;

    public QuestionOptionDTO() {}

    public QuestionOptionDTO(String optionCode) {
        this.optionCode = optionCode;
    }

    public QuestionOptionDTO(
            Integer id,
            Integer questionItemId,
            String content,
            String optionCode,
            Boolean correct,
            Integer position) {
        this.id = id;
        this.questionItemId = questionItemId;
        this.content = content;
        this.optionCode = optionCode;
        this.correct = correct;
        this.position = position;
    }

    public static QuestionOptionDTO convertEntityToDto(QuestionOptionEntity entity) {
        if (entity == null) return null;
        return new QuestionOptionDTO(
                entity.getId(),
                entity.getQuestionItemId(),
                entity.getContent(),
                entity.getOptionCode(),
                entity.getCorrect(),
                entity.getPosition());
    }

    public QuestionOptionEntity convertDtoToEntity() {
        QuestionOptionEntity entity =
                new QuestionOptionEntity(
                        this.questionItemId,
                        this.content,
                        this.optionCode,
                        this.correct,
                        this.position != null ? this.position : 0);
        if (this.id != null) entity.setId(this.id);
        return entity;
    }

    public void updateEntityFromDto(QuestionOptionEntity entity) {
        if (entity == null) return;
        entity.setContent(this.content);
        entity.setOptionCode(this.optionCode);
        entity.setCorrect(this.correct);
        if (this.position != null) entity.setPosition(this.position);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getQuestionItemId() {
        return questionItemId;
    }

    public void setQuestionItemId(Integer questionItemId) {
        this.questionItemId = questionItemId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getOptionCode() {
        return optionCode;
    }

    public void setOptionCode(String optionCode) {
        this.optionCode = optionCode;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

    public boolean isCorrect() {
        return Boolean.TRUE.equals(correct);
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String optionCode() {
        return getOptionCode();
    }

    public String content() {
        return getContent();
    }

    public Boolean correct() {
        return getCorrect();
    }
}

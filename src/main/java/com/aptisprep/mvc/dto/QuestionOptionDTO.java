package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class QuestionOptionDTO {
    private Integer id;
    private Integer questionItemId;
    private String content;
    private String optionCode;
    private Boolean correct;
    private int position;

    public QuestionOptionDTO() {}

    public QuestionOptionDTO(
            Integer id,
            Integer questionItemId,
            String content,
            String optionCode,
            Boolean correct,
            int position) {
        this.id = id;
        this.questionItemId = questionItemId;
        this.content = content;
        this.optionCode = optionCode;
        this.correct = correct;
        this.position = position;
    }

    public static QuestionOptionDTO convertEntityToDto(QuestionOption entity) {
        if (entity == null) return null;
        return new QuestionOptionDTO(
                entity.getId(),
                entity.getQuestionItemId(),
                entity.getContent(),
                entity.getOptionCode(),
                entity.getCorrect(),
                entity.getPosition());
    }

    public static QuestionOption convertDtoToEntity(QuestionOptionDTO dto) {
        if (dto == null) return null;
        return new QuestionOption(
                dto.getId(),
                dto.getQuestionItemId(),
                dto.getContent(),
                dto.getOptionCode(),
                dto.getCorrect(),
                dto.getPosition());
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

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}












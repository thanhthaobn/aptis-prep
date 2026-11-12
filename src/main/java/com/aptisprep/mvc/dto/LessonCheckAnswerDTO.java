package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class LessonCheckAnswerDTO {
    private Integer id;
    private LessonCheckAttempt attempt;
    private Integer questionId;
    private Integer selectedOption;

    public LessonCheckAnswerDTO() {}

    public LessonCheckAnswerDTO(
            Integer id,
            LessonCheckAttempt attempt,
            Integer questionId,
            Integer selectedOption) {
        this.id = id;
        this.attempt = attempt;
        this.questionId = questionId;
        this.selectedOption = selectedOption;
    }

    public static LessonCheckAnswerDTO convertEntityToDto(LessonCheckAnswer entity) {
        if (entity == null) return null;
        return new LessonCheckAnswerDTO(
                entity.getId(),
                entity.getAttempt(),
                entity.getQuestionId(),
                entity.getSelectedOption());
    }

    public static LessonCheckAnswer convertDtoToEntity(LessonCheckAnswerDTO dto) {
        if (dto == null) return null;
        return new LessonCheckAnswer(
                dto.getId(),
                dto.getAttempt(),
                dto.getQuestionId(),
                dto.getSelectedOption());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LessonCheckAttempt getAttempt() {
        return attempt;
    }

    public void setAttempt(LessonCheckAttempt attempt) {
        this.attempt = attempt;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public Integer getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(Integer selectedOption) {
        this.selectedOption = selectedOption;
    }
}












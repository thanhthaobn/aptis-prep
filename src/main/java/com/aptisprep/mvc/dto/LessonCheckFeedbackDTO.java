package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class LessonCheckFeedbackDTO {
    private Integer id;
    private LessonCheckAttempt attempt;
    private String prompt;
    private String selected;
    private String correct;
    private boolean rightAnswer;
    private String explanation;
    private String optionsText;

    public LessonCheckFeedbackDTO() {}

    public LessonCheckFeedbackDTO(
            Integer id,
            LessonCheckAttempt attempt,
            String prompt,
            String selected,
            String correct,
            boolean rightAnswer,
            String explanation,
            String optionsText) {
        this.id = id;
        this.attempt = attempt;
        this.prompt = prompt;
        this.selected = selected;
        this.correct = correct;
        this.rightAnswer = rightAnswer;
        this.explanation = explanation;
        this.optionsText = optionsText;
    }

    public static LessonCheckFeedbackDTO convertEntityToDto(LessonCheckFeedback entity) {
        if (entity == null) return null;
        return new LessonCheckFeedbackDTO(
                entity.getId(),
                entity.getAttempt(),
                entity.getPrompt(),
                entity.getSelected(),
                entity.getCorrect(),
                entity.isRightAnswer(),
                entity.getExplanation(),
                entity.getOptionsText());
    }

    public static LessonCheckFeedback convertDtoToEntity(LessonCheckFeedbackDTO dto) {
        if (dto == null) return null;
        return new LessonCheckFeedback(
                dto.getId(),
                dto.getAttempt(),
                dto.getPrompt(),
                dto.getSelected(),
                dto.getCorrect(),
                dto.isRightAnswer(),
                dto.getExplanation(),
                dto.getOptionsText());
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

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getSelected() {
        return selected;
    }

    public void setSelected(String selected) {
        this.selected = selected;
    }

    public String getCorrect() {
        return correct;
    }

    public void setCorrect(String correct) {
        this.correct = correct;
    }

    public boolean isRightAnswer() {
        return rightAnswer;
    }

    public void setRightAnswer(boolean rightAnswer) {
        this.rightAnswer = rightAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getOptionsText() {
        return optionsText;
    }

    public void setOptionsText(String optionsText) {
        this.optionsText = optionsText;
    }
}












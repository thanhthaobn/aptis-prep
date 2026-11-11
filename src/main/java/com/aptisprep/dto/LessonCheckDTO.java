package com.aptisprep.dto;

import com.aptisprep.entity.LessonCheck;
import jakarta.validation.constraints.Size;

public class LessonCheckDTO {
    private Integer id;
    private Integer lessonId;
    private int position;

    @Size(max = 2000)
    private String prompt;

    @Size(max = 1000)
    private String optionA;

    @Size(max = 1000)
    private String optionB;

    @Size(max = 1000)
    private String optionC;

    @Size(max = 1000)
    private String optionD;

    @Size(max = 1000)
    private String optionE;

    @Size(max = 1000)
    private String optionF;

    private Integer correctOption;

    @Size(max = 4000)
    private String explanation;

    public LessonCheckDTO() {}

    public LessonCheckDTO(
            Integer id,
            Integer lessonId,
            int position,
            String prompt,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            String optionE,
            String optionF,
            Integer correctOption,
            String explanation) {
        this.id = id;
        this.lessonId = lessonId;
        this.position = position;
        this.prompt = prompt;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.optionE = optionE;
        this.optionF = optionF;
        this.correctOption = correctOption;
        this.explanation = explanation;
    }

    public static LessonCheckDTO convertEntityToDto(LessonCheck entity) {
        if (entity == null) return null;
        return new LessonCheckDTO(
                entity.getId(),
                entity.getLessonId(),
                entity.getPosition(),
                entity.getPrompt(),
                entity.getOptionA(),
                entity.getOptionB(),
                entity.getOptionC(),
                entity.getOptionD(),
                entity.getOptionE(),
                entity.getOptionF(),
                entity.getCorrectOption(),
                entity.getExplanation());
    }

    public static LessonCheck convertDtoToEntity(LessonCheckDTO dto) {
        if (dto == null) return null;
        return new LessonCheck(
                dto.id,
                dto.lessonId,
                dto.position,
                dto.prompt,
                dto.optionA,
                dto.optionB,
                dto.optionC,
                dto.optionD,
                dto.optionE,
                dto.optionF,
                dto.correctOption == null ? 0 : dto.correctOption,
                dto.explanation);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getLessonId() {
        return lessonId;
    }

    public void setLessonId(Integer lessonId) {
        this.lessonId = lessonId;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getOptionA() {
        return optionA;
    }

    public void setOptionA(String optionA) {
        this.optionA = optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public void setOptionB(String optionB) {
        this.optionB = optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public void setOptionC(String optionC) {
        this.optionC = optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public void setOptionD(String optionD) {
        this.optionD = optionD;
    }

    public String getOptionE() {
        return optionE;
    }

    public void setOptionE(String optionE) {
        this.optionE = optionE;
    }

    public String getOptionF() {
        return optionF;
    }

    public void setOptionF(String optionF) {
        this.optionF = optionF;
    }

    public Integer getCorrectOption() {
        return correctOption;
    }

    public void setCorrectOption(Integer correctOption) {
        this.correctOption = correctOption;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}

package com.aptisprep.dto;

import com.aptisprep.entity.LessonCheck;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

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

    public LessonCheck convertDtoToEntity() {
        LessonCheck entity =
                new LessonCheck(
                        this.lessonId,
                        this.position,
                        this.prompt,
                        this.optionA,
                        this.optionB,
                        this.optionC,
                        this.optionD,
                        this.optionE,
                        this.optionF,
                        this.correctOption == null ? 0 : this.correctOption,
                        this.explanation);
        if (this.id != null) {
            entity.setId(this.id);
        }
        return entity;
    }

    public static LessonCheck convertDtoToEntity(LessonCheckDTO dto) {
        return dto == null ? null : dto.convertDtoToEntity();
    }

    public void updateEntityFromDto(LessonCheck entity) {
        if (entity == null) return;
        if (this.lessonId != null) entity.setLessonId(this.lessonId);
        entity.setPosition(this.position);
        entity.setPrompt(this.prompt);
        entity.setOptionA(this.optionA);
        entity.setOptionB(this.optionB);
        entity.setOptionC(this.optionC);
        entity.setOptionD(this.optionD);
        entity.setOptionE(this.optionE);
        entity.setOptionF(this.optionF);
        if (this.correctOption != null) entity.setCorrectOption(this.correctOption);
        entity.setExplanation(this.explanation);
    }

    public List<String> getOptions() {
        List<String> list = new ArrayList<>();
        if (optionA != null) list.add(optionA);
        if (optionB != null) list.add(optionB);
        if (optionC != null) list.add(optionC);
        if (optionD != null) list.add(optionD);
        if (optionE != null) list.add(optionE);
        if (optionF != null) list.add(optionF);
        return list;
    }

    public List<Integer> getOptionValues() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < getOptions().size(); i++) {
            list.add(i);
        }
        return list;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer id() {
        return id;
    }

    public Integer getLessonId() {
        return lessonId;
    }

    public void setLessonId(Integer lessonId) {
        this.lessonId = lessonId;
    }

    public Integer lessonId() {
        return lessonId;
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

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String prompt() {
        return prompt;
    }

    public String getOptionA() {
        return optionA;
    }

    public void setOptionA(String optionA) {
        this.optionA = optionA;
    }

    public String optionA() {
        return optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public void setOptionB(String optionB) {
        this.optionB = optionB;
    }

    public String optionB() {
        return optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public void setOptionC(String optionC) {
        this.optionC = optionC;
    }

    public String optionC() {
        return optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public void setOptionD(String optionD) {
        this.optionD = optionD;
    }

    public String optionD() {
        return optionD;
    }

    public String getOptionE() {
        return optionE;
    }

    public void setOptionE(String optionE) {
        this.optionE = optionE;
    }

    public String optionE() {
        return optionE;
    }

    public String getOptionF() {
        return optionF;
    }

    public void setOptionF(String optionF) {
        this.optionF = optionF;
    }

    public String optionF() {
        return optionF;
    }

    public Integer getCorrectOption() {
        return correctOption;
    }

    public void setCorrectOption(Integer correctOption) {
        this.correctOption = correctOption;
    }

    public Integer correctOption() {
        return correctOption;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String explanation() {
        return explanation;
    }
}

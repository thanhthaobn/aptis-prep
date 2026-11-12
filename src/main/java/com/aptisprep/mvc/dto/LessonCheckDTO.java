package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.LessonCheck;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class LessonCheckDTO {
    private Integer id;
    private Integer lessonId;
    private Integer position;

    @NotBlank(message = "Câu hỏi không được để trống")
    @Size(max = 2000, message = "Câu hỏi tối đa 2000 ký tự")
    private String prompt;

    @NotBlank(message = "Đáp án A không được để trống")
    @Size(max = 1000, message = "Đáp án A tối đa 1000 ký tự")
    private String optionA;

    @NotBlank(message = "Đáp án B không được để trống")
    @Size(max = 1000, message = "Đáp án B tối đa 1000 ký tự")
    private String optionB;

    @Size(max = 1000, message = "Đáp án C tối đa 1000 ký tự")
    private String optionC;

    @Size(max = 1000, message = "Đáp án D tối đa 1000 ký tự")
    private String optionD;

    @Size(max = 1000, message = "Đáp án E tối đa 1000 ký tự")
    private String optionE;

    @Size(max = 1000, message = "Đáp án F tối đa 1000 ký tự")
    private String optionF;

    @NotNull(message = "Phải chọn đáp án đúng")
    @Min(value = 0, message = "Đáp án đúng không hợp lệ")
    @Max(value = 5, message = "Đáp án đúng không hợp lệ")
    private Integer correctOption;

    @NotBlank(message = "Giải thích không được để trống")
    @Size(max = 4000, message = "Giải thích tối đa 4000 ký tự")
    private String explanation;

    public LessonCheckDTO() {
    }

    public LessonCheckDTO(
            Integer id,
            Integer lessonId,
            Integer position,
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

    public static LessonCheckDTO convertEntityToDto(LessonCheck lessonCheck) {
        return new LessonCheckDTO(
                lessonCheck.getId(),
                lessonCheck.getLessonId(),
                lessonCheck.getPosition(),
                lessonCheck.getPrompt(),
                lessonCheck.getOptionA(),
                lessonCheck.getOptionB(),
                lessonCheck.getOptionC(),
                lessonCheck.getOptionD(),
                lessonCheck.getOptionE(),
                lessonCheck.getOptionF(),
                lessonCheck.getCorrectOption(),
                lessonCheck.getExplanation());
    }

    public static LessonCheck convertDtoToEntity(LessonCheckDTO lessonCheckDTO) {
        return new LessonCheck(
                lessonCheckDTO.id,
                lessonCheckDTO.lessonId,
                lessonCheckDTO.position == null ? 0 : lessonCheckDTO.position,
                lessonCheckDTO.prompt,
                lessonCheckDTO.optionA,
                lessonCheckDTO.optionB,
                lessonCheckDTO.optionC,
                lessonCheckDTO.optionD,
                lessonCheckDTO.optionE,
                lessonCheckDTO.optionF,
                lessonCheckDTO.correctOption == null ? 0 : lessonCheckDTO.correctOption,
                lessonCheckDTO.explanation);
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

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
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

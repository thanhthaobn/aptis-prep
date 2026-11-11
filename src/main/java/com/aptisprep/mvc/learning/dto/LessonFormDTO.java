package com.aptisprep.mvc.learning.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

public class LessonFormDTO extends LessonMaterialDTO {
    @Valid private List<CheckForm> checks = new ArrayList<>();

    public List<CheckForm> getChecks() {
        return checks;
    }

    public void setChecks(List<CheckForm> value) {
        checks = value;
    }

    public static class CheckForm {
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

        public Integer getCorrectOption() {
            return correctOption;
        }

        public void setCorrectOption(Integer v) {
            correctOption = v;
        }

        public String getExplanation() {
            return explanation;
        }

        public void setExplanation(String v) {
            explanation = v;
        }
    }

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 10000)
    private String description;

    @NotBlank private String skill;
    @NotBlank private String cefrLevel;

    @NotNull
    @Min(1)
    private Integer displayOrder;

    @NotBlank private String status;

    @Size(max = 10000)
    private String transcript;

    @Min(1)
    @Max(5000)
    private Integer minWordCount;

    @Min(1)
    @Max(5000)
    private Integer maxWordCount;

    public String getTitle() {
        return title;
    }

    public void setTitle(String v) {
        title = v;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String v) {
        description = v;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String v) {
        skill = v;
    }

    public String getCefrLevel() {
        return cefrLevel;
    }

    public void setCefrLevel(String v) {
        cefrLevel = v;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer v) {
        displayOrder = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String v) {
        transcript = v;
    }

    public Integer getMinWordCount() {
        return minWordCount;
    }

    public void setMinWordCount(Integer v) {
        minWordCount = v;
    }

    public Integer getMaxWordCount() {
        return maxWordCount;
    }

    public void setMaxWordCount(Integer v) {
        maxWordCount = v;
    }
}

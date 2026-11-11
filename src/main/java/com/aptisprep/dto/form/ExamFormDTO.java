package com.aptisprep.dto.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

public class ExamFormDTO {
    public ExamFormDTO() {}

    public ExamFormDTO(
            String title,
            String description,
            String examType,
            String targetLevel,
            String skill,
            Integer durationMinutes,
            Integer certificateId,
            Integer scoringSchemeId,
            String sectionSkill,
            Integer sectionDurationMinutes,
            String questionIds,
            String status,
            boolean demoOnly,
            List<SectionForm> sections) {
        this.title = title;
        this.description = description;
        this.examType = examType;
        this.targetLevel = targetLevel;
        this.skill = skill;
        this.durationMinutes = durationMinutes;
        this.certificateId = certificateId;
        this.scoringSchemeId = scoringSchemeId;
        this.sectionSkill = sectionSkill;
        this.sectionDurationMinutes = sectionDurationMinutes;
        this.questionIds = questionIds;
        this.status = status;
        this.demoOnly = demoOnly;
        this.sections = sections;
    }

    @NotBlank(message = "Tiêu đề đề thi không được để trống")
    @Size(max = 255)
    private String title;

    @Size(max = 4000)
    private String description;

    @NotBlank
    @Size(max = 30)
    private String examType = "PRACTICE";

    @Size(max = 20)
    private String targetLevel = "B1";

    @NotBlank
    @Size(max = 30)
    private String skill = "READING";

    @Positive private Integer durationMinutes;

    @Positive private Integer certificateId;

    @Positive private Integer scoringSchemeId;

    @Size(max = 30)
    private String sectionSkill = "READING";

    @Positive private Integer sectionDurationMinutes = 35;

    @Size(max = 4000)
    private String questionIds;

    @NotBlank
    @Size(max = 30)
    private String status = "DRAFT";

    private boolean demoOnly;

    public boolean isDemoOnly() {
        return demoOnly;
    }

    public void setDemoOnly(boolean value) {
        demoOnly = value;
    }

    private List<@Valid SectionForm> sections = new ArrayList<>();

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

    public String getExamType() {
        return examType;
    }

    public void setExamType(String v) {
        examType = v;
    }

    public String getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(String v) {
        targetLevel = v;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String v) {
        skill = v;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer v) {
        durationMinutes = v;
    }

    public Integer getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(Integer v) {
        certificateId = v;
    }

    public Integer getScoringSchemeId() {
        return scoringSchemeId;
    }

    public void setScoringSchemeId(Integer v) {
        scoringSchemeId = v;
    }

    public String getSectionSkill() {
        return sectionSkill;
    }

    public void setSectionSkill(String v) {
        sectionSkill = v;
    }

    public Integer getSectionDurationMinutes() {
        return sectionDurationMinutes;
    }

    public void setSectionDurationMinutes(Integer v) {
        sectionDurationMinutes = v;
    }

    public String getQuestionIds() {
        return questionIds;
    }

    public void setQuestionIds(String v) {
        questionIds = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public List<SectionForm> getSections() {
        return sections;
    }

    public void setSections(List<SectionForm> value) {
        sections = value == null ? new ArrayList<>() : value;
    }

    public static class SectionForm {
        @NotBlank
        @Size(max = 30)
        private String skill;

        @Positive private Integer durationMinutes;

        @Size(max = 4000)
        private String questionIds;

        public String getSkill() {
            return skill;
        }

        public void setSkill(String value) {
            skill = value;
        }

        public Integer getDurationMinutes() {
            return durationMinutes;
        }

        public void setDurationMinutes(Integer value) {
            durationMinutes = value;
        }

        public String getQuestionIds() {
            return questionIds;
        }

        public void setQuestionIds(String value) {
            questionIds = value;
        }
    }
}

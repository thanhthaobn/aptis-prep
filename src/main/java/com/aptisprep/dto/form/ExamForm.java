package com.aptisprep.dto.form;

public class ExamForm {
    private String title;
    private String description;
    private String examType = "PRACTICE";
    private String targetLevel = "B1";
    private String skill = "READING";
    private Integer durationMinutes = 35;
    private Integer certificateId;
    private Integer scoringSchemeId;
    private String sectionSkill = "READING";
    private Integer sectionDurationMinutes = 35;
    private String questionIds;
    private String status = "DRAFT";
    public String getTitle() { return title; } public void setTitle(String v) { title = v; }
    public String getDescription() { return description; } public void setDescription(String v) { description = v; }
    public String getExamType() { return examType; } public void setExamType(String v) { examType = v; }
    public String getTargetLevel() { return targetLevel; } public void setTargetLevel(String v) { targetLevel = v; }
    public String getSkill() { return skill; } public void setSkill(String v) { skill = v; }
    public Integer getDurationMinutes() { return durationMinutes; } public void setDurationMinutes(Integer v) { durationMinutes = v; }
    public Integer getCertificateId() { return certificateId; } public void setCertificateId(Integer v) { certificateId = v; }
    public Integer getScoringSchemeId() { return scoringSchemeId; } public void setScoringSchemeId(Integer v) { scoringSchemeId = v; }
    public String getSectionSkill() { return sectionSkill; } public void setSectionSkill(String v) { sectionSkill = v; }
    public Integer getSectionDurationMinutes() { return sectionDurationMinutes; } public void setSectionDurationMinutes(Integer v) { sectionDurationMinutes = v; }
    public String getQuestionIds() { return questionIds; } public void setQuestionIds(String v) { questionIds = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
}

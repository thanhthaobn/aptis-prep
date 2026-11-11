package com.aptisprep.dto;

import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.presentation.form.ExamFormDTO;

/** DTO boundary for the scalar fields of a mock exam entity. */
public class MockExamEntityDTO {
    public MockExamEntityDTO() {}

    public MockExamEntityDTO(
            String title,
            String description,
            String examType,
            String targetLevel,
            String skill,
            Integer durationMinutes,
            Integer certificateId,
            Integer scoringSchemeId,
            String status,
            boolean demoOnly) {
        this.title = title;
        this.description = description;
        this.examType = examType;
        this.targetLevel = targetLevel;
        this.skill = skill;
        this.durationMinutes = durationMinutes;
        this.certificateId = certificateId;
        this.scoringSchemeId = scoringSchemeId;
        this.status = status;
        this.demoOnly = demoOnly;
    }

    private String title;
    private String description;
    private String examType;
    private String targetLevel;
    private String skill;
    private Integer durationMinutes;
    private Integer certificateId;
    private Integer scoringSchemeId;
    private String status;
    private boolean demoOnly;

    public static MockExamEntityDTO fromForm(ExamFormDTO form) {
        if (form == null) return null;
        MockExamEntityDTO dto = new MockExamEntityDTO();
        dto.title = form.getTitle();
        dto.description = form.getDescription();
        dto.examType = form.getExamType();
        dto.targetLevel = form.getTargetLevel();
        dto.skill = form.getSkill();
        dto.durationMinutes = form.getDurationMinutes();
        dto.certificateId = form.getCertificateId();
        dto.scoringSchemeId = form.getScoringSchemeId();
        dto.status = form.getStatus();
        dto.demoOnly = form.isDemoOnly();
        return dto;
    }

    public static MockExamEntity convertDtoToEntity(MockExamEntityDTO dto) {
        if (dto == null) return null;
        MockExamEntity entity = new MockExamEntity();
        entity.setTitle(trim(dto.title));
        entity.setDescription(blankToNull(dto.description));
        entity.setExamType(dto.examType);
        entity.setTargetLevel(blankToNull(dto.targetLevel));
        entity.setSkill(dto.skill);
        entity.setDurationMinutes(dto.durationMinutes);
        entity.setCertificateId(dto.certificateId);
        entity.setScoringSchemeId(dto.scoringSchemeId);
        entity.setStatus(dto.status);
        entity.setDemoOnly(dto.demoOnly);
        return entity;
    }

    public static MockExamEntity convertDtoToEntity(ExamFormDTO form) {
        return convertDtoToEntity(fromForm(form));
    }

    public static MockExamEntityDTO convertEntityToDto(MockExamEntity entity) {
        if (entity == null) return null;
        MockExamEntityDTO dto = new MockExamEntityDTO();
        dto.title = entity.getTitle();
        dto.description = entity.getDescription();
        dto.examType = entity.getExamType();
        dto.targetLevel = entity.getTargetLevel();
        dto.skill = entity.getSkill();
        dto.durationMinutes = entity.getDurationMinutes();
        dto.certificateId = entity.getCertificateId();
        dto.scoringSchemeId = entity.getScoringSchemeId();
        dto.status = entity.getStatus();
        dto.demoOnly = entity.isDemoOnly();
        return dto;
    }

    public ExamFormDTO toForm() {
        ExamFormDTO form = new ExamFormDTO();
        form.setTitle(title);
        form.setDescription(description);
        form.setExamType(examType);
        form.setTargetLevel(targetLevel);
        form.setSkill(skill);
        form.setDurationMinutes(durationMinutes);
        form.setCertificateId(certificateId);
        form.setScoringSchemeId(scoringSchemeId);
        form.setStatus(status);
        form.setDemoOnly(demoOnly);
        return form;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String blankToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getExamType() {
        return examType;
    }

    public String getTargetLevel() {
        return targetLevel;
    }

    public String getSkill() {
        return skill;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public Integer getCertificateId() {
        return certificateId;
    }

    public Integer getScoringSchemeId() {
        return scoringSchemeId;
    }

    public String getStatus() {
        return status;
    }

    public boolean isDemoOnly() {
        return demoOnly;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setExamType(String examType) {
        this.examType = examType;
    }

    public void setTargetLevel(String targetLevel) {
        this.targetLevel = targetLevel;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public void setCertificateId(Integer certificateId) {
        this.certificateId = certificateId;
    }

    public void setScoringSchemeId(Integer scoringSchemeId) {
        this.scoringSchemeId = scoringSchemeId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDemoOnly(boolean demoOnly) {
        this.demoOnly = demoOnly;
    }
}

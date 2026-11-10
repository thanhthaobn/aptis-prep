package com.aptisprep.dto;

import com.aptisprep.dto.form.ExamForm;
import com.aptisprep.entity.MockExamEntity;

/** DTO boundary for the scalar fields of a mock exam entity. */
public class MockExamEntityDto {
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

    public static MockExamEntityDto fromForm(ExamForm form) {
        if (form == null) return null;
        MockExamEntityDto dto = new MockExamEntityDto();
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

    public static MockExamEntity convertDtoToEntity(MockExamEntityDto dto) {
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

    public static MockExamEntity convertDtoToEntity(ExamForm form) {
        return convertDtoToEntity(fromForm(form));
    }

    public static MockExamEntityDto convertEntityToDto(MockExamEntity entity) {
        if (entity == null) return null;
        MockExamEntityDto dto = new MockExamEntityDto();
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

    public ExamForm toForm() {
        ExamForm form = new ExamForm();
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
}

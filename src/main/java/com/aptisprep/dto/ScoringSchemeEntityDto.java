package com.aptisprep.dto;

import com.aptisprep.dto.form.ScoringSchemeForm;
import com.aptisprep.entity.Certificate;
import com.aptisprep.entity.ScoringScheme;

/** DTO boundary for a scoring-scheme entity. */
public class ScoringSchemeEntityDto {
    private Certificate certificate;
    private String name;
    private String version;
    private String examType;
    private String scopeSkill;

    public static ScoringSchemeEntityDto fromForm(ScoringSchemeForm form, Certificate certificate) {
        if (form == null) return null;
        ScoringSchemeEntityDto dto = new ScoringSchemeEntityDto();
        dto.certificate = certificate;
        dto.name = form.getName();
        dto.version = form.getVersion();
        dto.examType = form.getExamType();
        dto.scopeSkill = form.getScopeSkill();
        return dto;
    }

    public static ScoringScheme convertDtoToEntity(ScoringSchemeEntityDto dto) {
        if (dto == null) return null;
        ScoringScheme entity = new ScoringScheme();
        entity.setCertificate(dto.certificate);
        entity.setName(trim(dto.name));
        entity.setVersion(trim(dto.version));
        entity.setExamType(blankToNull(dto.examType));
        entity.setScopeSkill(blankToNull(dto.scopeSkill));
        return entity;
    }

    public static ScoringScheme convertDtoToEntity(
            ScoringSchemeForm form, Certificate certificate) {
        return convertDtoToEntity(fromForm(form, certificate));
    }

    public static ScoringSchemeEntityDto convertEntityToDto(ScoringScheme entity) {
        if (entity == null) return null;
        ScoringSchemeEntityDto dto = new ScoringSchemeEntityDto();
        dto.certificate = entity.getCertificate();
        dto.name = entity.getName();
        dto.version = entity.getVersion();
        dto.examType = entity.getExamType();
        dto.scopeSkill = entity.getScopeSkill();
        return dto;
    }

    public ScoringSchemeForm toForm() {
        ScoringSchemeForm form = new ScoringSchemeForm();
        form.setCertificateId(certificate == null ? null : certificate.getId());
        form.setName(name);
        form.setVersion(version);
        form.setExamType(examType);
        form.setScopeSkill(scopeSkill);
        return form;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String blankToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getExamType() {
        return examType;
    }

    public String getScopeSkill() {
        return scopeSkill;
    }
}

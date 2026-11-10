package com.aptisprep.mvc.scoring.dto;

import com.aptisprep.mvc.scoring.entity.Certificate;
import com.aptisprep.mvc.scoring.entity.ScoringScheme;

/** DTO boundary for a scoring-scheme entity. */
public class ScoringSchemeEntityDTO {
    private Certificate certificate;
    private String name;
    private String version;
    private String examType;
    private String scopeSkill;

    public static ScoringSchemeEntityDTO fromForm(
            ScoringSchemeFormDTO form, Certificate certificate) {
        if (form == null) return null;
        ScoringSchemeEntityDTO dto = new ScoringSchemeEntityDTO();
        dto.certificate = certificate;
        dto.name = form.getName();
        dto.version = form.getVersion();
        dto.examType = form.getExamType();
        dto.scopeSkill = form.getScopeSkill();
        return dto;
    }

    public static ScoringScheme convertDtoToEntity(ScoringSchemeEntityDTO dto) {
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
            ScoringSchemeFormDTO form, Certificate certificate) {
        return convertDtoToEntity(fromForm(form, certificate));
    }

    public static ScoringSchemeEntityDTO convertEntityToDto(ScoringScheme entity) {
        if (entity == null) return null;
        ScoringSchemeEntityDTO dto = new ScoringSchemeEntityDTO();
        dto.certificate = entity.getCertificate();
        dto.name = entity.getName();
        dto.version = entity.getVersion();
        dto.examType = entity.getExamType();
        dto.scopeSkill = entity.getScopeSkill();
        return dto;
    }

    public ScoringSchemeFormDTO toForm() {
        ScoringSchemeFormDTO form = new ScoringSchemeFormDTO();
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

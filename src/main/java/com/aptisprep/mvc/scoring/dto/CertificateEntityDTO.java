package com.aptisprep.mvc.scoring.dto;

import com.aptisprep.mvc.scoring.entity.Certificate;

/** DTO boundary for a certificate entity. */
public class CertificateEntityDTO {
    private String name;
    private String code;
    private String description;

    public static CertificateEntityDTO fromForm(CertificateFormDTO form) {
        if (form == null) return null;
        CertificateEntityDTO dto = new CertificateEntityDTO();
        dto.name = form.getName();
        dto.code = form.getCode();
        dto.description = form.getDescription();
        return dto;
    }

    public static Certificate convertDtoToEntity(CertificateEntityDTO dto) {
        if (dto == null) return null;
        Certificate entity = new Certificate();
        entity.setName(trim(dto.name));
        entity.setCode(trim(dto.code));
        entity.setDescription(blankToNull(dto.description));
        return entity;
    }

    public static Certificate convertDtoToEntity(CertificateFormDTO form) {
        return convertDtoToEntity(fromForm(form));
    }

    public static CertificateEntityDTO convertEntityToDto(Certificate entity) {
        if (entity == null) return null;
        CertificateEntityDTO dto = new CertificateEntityDTO();
        dto.name = entity.getName();
        dto.code = entity.getCode();
        dto.description = entity.getDescription();
        return dto;
    }

    public CertificateFormDTO toForm() {
        CertificateFormDTO form = new CertificateFormDTO();
        form.setName(name);
        form.setCode(code);
        form.setDescription(description);
        return form;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String blankToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}

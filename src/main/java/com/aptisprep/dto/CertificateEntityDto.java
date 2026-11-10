package com.aptisprep.dto;

import com.aptisprep.dto.form.CertificateForm;
import com.aptisprep.entity.Certificate;

/** DTO boundary for a certificate entity. */
public class CertificateEntityDto {
    private String name;
    private String code;
    private String description;

    public static CertificateEntityDto fromForm(CertificateForm form) {
        if (form == null) return null;
        CertificateEntityDto dto = new CertificateEntityDto();
        dto.name = form.getName();
        dto.code = form.getCode();
        dto.description = form.getDescription();
        return dto;
    }

    public static Certificate convertDtoToEntity(CertificateEntityDto dto) {
        if (dto == null) return null;
        Certificate entity = new Certificate();
        entity.setName(trim(dto.name));
        entity.setCode(trim(dto.code));
        entity.setDescription(blankToNull(dto.description));
        return entity;
    }

    public static Certificate convertDtoToEntity(CertificateForm form) {
        return convertDtoToEntity(fromForm(form));
    }

    public static CertificateEntityDto convertEntityToDto(Certificate entity) {
        if (entity == null) return null;
        CertificateEntityDto dto = new CertificateEntityDto();
        dto.name = entity.getName();
        dto.code = entity.getCode();
        dto.description = entity.getDescription();
        return dto;
    }

    public CertificateForm toForm() {
        CertificateForm form = new CertificateForm();
        form.setName(name);
        form.setCode(code);
        form.setDescription(description);
        return form;
    }

    private static String trim(String value) { return value == null ? null : value.trim(); }

    private static String blankToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
}

package com.aptisprep.dto.view;

import com.aptisprep.dto.form.CertificateFormDTO;
import com.aptisprep.entity.Certificate;

/** DTO boundary for a certificate entity. */
public class CertificateDTO {
    public CertificateDTO() {}

    public CertificateDTO(String name, String code, String description) {
        this.name = name;
        this.code = code;
        this.description = description;
    }

    private String name;
    private String code;
    private String description;

    public static CertificateDTO fromForm(CertificateFormDTO form) {
        if (form == null) return null;
        CertificateDTO dto = new CertificateDTO();
        dto.name = form.getName();
        dto.code = form.getCode();
        dto.description = form.getDescription();
        return dto;
    }

    public static Certificate convertDtoToEntity(CertificateDTO dto) {
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

    public static CertificateDTO convertEntityToDto(Certificate entity) {
        if (entity == null) return null;
        CertificateDTO dto = new CertificateDTO();
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

    public void setName(String name) {
        this.name = name;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

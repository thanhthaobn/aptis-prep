package com.aptisprep.dto;

import com.aptisprep.entity.Certificate;
import com.aptisprep.enums.RecordStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Locale;

public class CertificateDTO {
    private Integer id;

    @NotBlank(message = "Tên chứng chỉ không được để trống")
    @Size(max = 255, message = "Tên chứng chỉ tối đa 255 ký tự")
    private String name;

    @NotBlank(message = "Mã chứng chỉ không được để trống")
    @Size(max = 50, message = "Mã chứng chỉ tối đa 50 ký tự")
    private String code;

    @Size(max = 4000, message = "Mô tả quá dài")
    private String description;

    @NotBlank(message = "Trạng thái không được để trống")
    private String status = "DRAFT";

    private Integer createdById;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CertificateDTO() {}

    public CertificateDTO(
            Integer id,
            String name,
            String code,
            String description,
            String status,
            Integer createdById,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.description = description;
        this.status = status;
        this.createdById = createdById;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CertificateDTO convertEntityToDto(Certificate entity) {
        if (entity == null) return null;
        return new CertificateDTO(
                entity.getId(),
                entity.getName(),
                entity.getCode(),
                entity.getDescription(),
                entity.getStatus() == null ? null : entity.getStatus().name(),
                entity.getCreatedBy() == null ? null : entity.getCreatedBy().getId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static Certificate convertDtoToEntity(CertificateDTO dto) {
        if (dto == null) return null;
        Certificate entity = new Certificate();
        entity.setId(dto.id);
        entity.setName(trim(dto.name));
        entity.setCode(trim(dto.code));
        entity.setDescription(blankToNull(dto.description));
        entity.setStatus(
                dto.status == null || dto.status.isBlank()
                        ? RecordStatus.DRAFT
                        : RecordStatus.valueOf(dto.status.trim().toUpperCase(Locale.ROOT)));
        entity.setCreatedAt(dto.createdAt);
        entity.setUpdatedAt(dto.updatedAt);
        return entity;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String blankToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Integer createdById) {
        this.createdById = createdById;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

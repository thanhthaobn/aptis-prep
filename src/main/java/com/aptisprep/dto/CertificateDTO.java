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

    private String statusLabel;
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
        this.statusLabel = "ACTIVE".equalsIgnoreCase(status) ? "Đang áp dụng" : "Ngừng áp dụng";
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

    public Certificate convertDtoToEntity() {
        Certificate entity = new Certificate();
        entity.setName(trim(this.name));
        entity.setCode(trim(this.code));
        entity.setDescription(blankToNull(this.description));
        entity.setStatus(parseStatus(this.status));
        entity.setCreatedAt(this.createdAt);
        entity.setUpdatedAt(this.updatedAt);
        if (this.id != null) entity.setId(this.id);
        return entity;
    }

    public static Certificate convertDtoToEntity(CertificateDTO dto) {
        return dto == null ? null : dto.convertDtoToEntity();
    }

    public void updateEntityFromDto(Certificate entity) {
        if (entity == null) return;
        entity.setName(trim(this.name));
        entity.setCode(trim(this.code));
        entity.setDescription(blankToNull(this.description));
        if (this.status != null && !this.status.isBlank()) {
            entity.setStatus(parseStatus(this.status));
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String blankToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed;
    }

    private static RecordStatus parseStatus(String s) {
        if (s == null || s.isBlank()) return RecordStatus.DRAFT;
        try {
            return RecordStatus.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return RecordStatus.DRAFT;
        }
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
        this.statusLabel = "ACTIVE".equalsIgnoreCase(status) ? "Đang áp dụng" : "Ngừng áp dụng";
    }

    public String getStatusLabel() {
        return statusLabel != null
                ? statusLabel
                : ("ACTIVE".equalsIgnoreCase(status) ? "Đang áp dụng" : "Ngừng áp dụng");
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
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

    public Integer id() {
        return getId();
    }

    public String name() {
        return getName();
    }

    public String code() {
        return getCode();
    }

    public String description() {
        return getDescription();
    }

    public String status() {
        return getStatus();
    }

    public String statusLabel() {
        return getStatusLabel();
    }
}

package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.*;
import java.time.*;
import java.util.*;

public class CertificateDTO {
    private Integer id;
    private User createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    public CertificateDTO() {}

    public CertificateDTO(
            String name,
            String code,
            String description,
            String status,
            Integer id,
            User createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.name = name;
        this.code = code;
        this.description = description;
        this.status = status;
        this.id = id;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CertificateDTO convertEntityToDto(Certificate entity) {
        if (entity == null) return null;
        return new CertificateDTO(
                entity.getName(),
                entity.getCode(),
                entity.getDescription(),
                entity.getStatus() == null ? null : entity.getStatus().name(),
                entity.getId(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static Certificate convertDtoToEntity(CertificateDTO dto) {
        if (dto == null) return null;
        return new Certificate(
                dto.getName(),
                dto.getCode(),
                dto.getDescription(),
                dto.getStatus() == null ? null : RecordStatus.valueOf(dto.getStatus()),
                dto.getId(),
                dto.getCreatedBy(),
                dto.getCreatedAt(),
                dto.getUpdatedAt());
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public void setName(String value) {
        name = value;
    }

    public void setCode(String value) {
        code = value;
    }

    public void setDescription(String value) {
        description = value;
    }

    public void setStatus(String value) {
        status = value;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer value) {
        this.id = value;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User value) {
        this.createdBy = value;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime value) {
        this.createdAt = value;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime value) {
        this.updatedAt = value;
    }
}

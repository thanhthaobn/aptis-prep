package com.aptisprep.dto;

import com.aptisprep.entity.Certificate;
import com.aptisprep.entity.ScoringScheme;
import com.aptisprep.enums.RecordStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ScoringSchemeDTO {
    private Integer id;

    @NotNull(message = "Phải chọn chứng chỉ")
    private Integer certificateId;

    private String certificateName;

    @NotBlank(message = "Tên bộ chấm điểm không được để trống")
    @Size(max = 255, message = "Tên bộ chấm điểm tối đa 255 ký tự")
    private String name;

    @NotBlank(message = "Phiên bản không được để trống")
    @Size(max = 50, message = "Phiên bản tối đa 50 ký tự")
    private String version;

    private String examType = "FULL";
    private String scopeSkill;
    private String status = "DRAFT";
    private String statusLabel;
    private LocalDateTime effectiveFrom;
    private LocalDateTime createdAt;

    private List<ScoreBandDTO> bands = new ArrayList<>();
    private List<ScoringScaleRuleDTO> rules = new ArrayList<>();
    private List<String> supportedSkills = new ArrayList<>();

    public ScoringSchemeDTO() {}

    public ScoringSchemeDTO(
            Integer id,
            Integer certificateId,
            String certificateName,
            String name,
            String version,
            String examType,
            String scopeSkill,
            String status,
            LocalDateTime effectiveFrom,
            LocalDateTime createdAt) {
        this.id = id;
        this.certificateId = certificateId;
        this.certificateName = certificateName;
        this.name = name;
        this.version = version;
        this.examType = examType;
        this.scopeSkill = scopeSkill;
        this.status = status;
        this.statusLabel = "ACTIVE".equalsIgnoreCase(status) ? "Đang áp dụng" : "Bản nháp";
        this.effectiveFrom = effectiveFrom;
        this.createdAt = createdAt;
    }

    public static ScoringSchemeDTO convertEntityToDto(ScoringScheme entity) {
        if (entity == null) return null;
        return new ScoringSchemeDTO(
                entity.getId(),
                entity.getCertificate() == null ? null : entity.getCertificate().getId(),
                entity.getCertificate() == null ? null : entity.getCertificate().getName(),
                entity.getName(),
                entity.getVersion(),
                entity.getExamType(),
                entity.getScopeSkill(),
                entity.getStatus() == null ? null : entity.getStatus().name(),
                entity.getEffectiveFrom(),
                entity.getCreatedAt());
    }

    public ScoringScheme convertDtoToEntity() {
        return convertDtoToEntity(certificateReference(this.certificateId));
    }

    public ScoringScheme convertDtoToEntity(Certificate cert) {
        ScoringScheme entity =
                new ScoringScheme(
                        cert,
                        this.name == null ? null : this.name.trim(),
                        this.version == null ? null : this.version.trim(),
                        this.examType,
                        this.scopeSkill,
                        parseStatus(this.status),
                        this.effectiveFrom,
                        this.createdAt);
        if (this.id != null) entity.setId(this.id);
        return entity;
    }

    public static ScoringScheme convertDtoToEntity(ScoringSchemeDTO dto, Certificate cert) {
        return dto == null ? null : dto.convertDtoToEntity(cert);
    }

    public static ScoringScheme convertDtoToEntity(ScoringSchemeFormDTO form, Certificate cert) {
        if (form == null) return null;
        ScoringSchemeDTO dto = new ScoringSchemeDTO();
        dto.setCertificateId(form.getCertificateId());
        dto.setName(form.getName());
        dto.setVersion(form.getVersion());
        dto.setStatus(form.getStatus());
        dto.setExamType(form.getExamType());
        dto.setScopeSkill(form.getScopeSkill());
        return dto.convertDtoToEntity(cert);
    }

    public void updateEntityFromDto(ScoringScheme entity) {
        if (entity == null) return;
        entity.setName(this.name == null ? null : this.name.trim());
        entity.setVersion(this.version == null ? null : this.version.trim());
        entity.setExamType(this.examType);
        entity.setScopeSkill(this.scopeSkill);
        if (this.status != null && !this.status.isBlank()) {
            entity.setStatus(parseStatus(this.status));
        }
        if (this.effectiveFrom != null) entity.setEffectiveFrom(this.effectiveFrom);
    }

    private static Certificate certificateReference(Integer id) {
        if (id == null) return null;
        Certificate c = new Certificate();
        c.setId(id);
        return c;
    }

    private static RecordStatus parseStatus(String s) {
        if (s == null || s.isBlank()) return RecordStatus.DRAFT;
        try {
            return RecordStatus.valueOf(s.trim().toUpperCase());
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

    public Integer getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(Integer certificateId) {
        this.certificateId = certificateId;
    }

    public String getCertificateName() {
        return certificateName;
    }

    public void setCertificateName(String certificateName) {
        this.certificateName = certificateName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getExamType() {
        return examType;
    }

    public void setExamType(String examType) {
        this.examType = examType;
    }

    public String getScopeSkill() {
        return scopeSkill;
    }

    public void setScopeSkill(String scopeSkill) {
        this.scopeSkill = scopeSkill;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.statusLabel = "ACTIVE".equalsIgnoreCase(status) ? "Đang áp dụng" : "Bản nháp";
    }

    public String getStatusLabel() {
        return statusLabel != null
                ? statusLabel
                : ("ACTIVE".equalsIgnoreCase(status) ? "Đang áp dụng" : "Bản nháp");
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDateTime effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<ScoreBandDTO> getBands() {
        return bands;
    }

    public void setBands(List<ScoreBandDTO> bands) {
        this.bands = bands;
    }

    public List<ScoringScaleRuleDTO> getRules() {
        return rules;
    }

    public void setRules(List<ScoringScaleRuleDTO> rules) {
        this.rules = rules;
    }

    public List<String> getSupportedSkills() {
        return supportedSkills;
    }

    public void setSupportedSkills(List<String> supportedSkills) {
        this.supportedSkills = supportedSkills;
    }

    public Integer id() {
        return getId();
    }

    public Integer certificateId() {
        return getCertificateId();
    }

    public String certificateName() {
        return getCertificateName();
    }

    public String name() {
        return getName();
    }

    public String version() {
        return getVersion();
    }

    public String examType() {
        return getExamType();
    }

    public String scopeSkill() {
        return getScopeSkill();
    }

    public String status() {
        return getStatus();
    }

    public String statusLabel() {
        return getStatusLabel();
    }

    public LocalDateTime effectiveFrom() {
        return getEffectiveFrom();
    }

    public List<String> supportedSkills() {
        return getSupportedSkills();
    }
}

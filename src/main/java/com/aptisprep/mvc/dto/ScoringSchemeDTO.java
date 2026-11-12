package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class ScoringSchemeDTO {
    private Integer id;
    private Certificate certificate;
    private LocalDateTime effectiveFrom;
    private LocalDateTime createdAt;
    private Integer certificateId;

    @NotBlank(message = "Tên bộ chấm điểm không được để trống")
    @Size(max = 255, message = "Tên bộ chấm điểm tối đa 255 ký tự")
    private String name;

    @NotBlank(message = "Phiên bản không được để trống")
    @Size(max = 30, message = "Phiên bản tối đa 30 ký tự")
    private String version;

    @NotBlank(message = "Trạng thái không được để trống")
    private String status = "DRAFT";

    private String examType;
    private String scopeSkill;
    private boolean configureScores;
    private ScoringScaleRuleDTO scale = new ScoringScaleRuleDTO();
    private Map<String, ScoringScaleRuleDTO> skillScales = new LinkedHashMap<>();
    private Map<String, ScoreBandDTO> ranges = new LinkedHashMap<>();
    private String targetLevel;
    private BigDecimal passingScore;
    private List<String> skills = new ArrayList<>();

    public ScoringSchemeDTO(
            Integer id,
            Certificate certificate,
            LocalDateTime effectiveFrom,
            LocalDateTime createdAt,
            Integer certificateId,
            String name,
            String version,
            String status,
            String examType,
            String scopeSkill,
            boolean configureScores,
            ScoringScaleRuleDTO scale,
            Map<String, ScoringScaleRuleDTO> skillScales,
            Map<String, ScoreBandDTO> ranges,
            String targetLevel,
            BigDecimal passingScore) {
        this.id = id;
        this.certificate = certificate;
        this.effectiveFrom = effectiveFrom;
        this.createdAt = createdAt;
        this.certificateId = certificateId;
        this.name = name;
        this.version = version;
        this.status = status;
        this.examType = examType;
        this.scopeSkill = scopeSkill;
        this.configureScores = configureScores;
        this.scale = scale;
        this.skillScales = skillScales;
        this.ranges = ranges;
        this.targetLevel = targetLevel;
        this.passingScore = passingScore;
    }

    public ScoringSchemeDTO() {
        for (String skill : com.aptisprep.mvc.enums.Skill.codes()) {
            skillScales.put(skill, new ScoringScaleRuleDTO());
            for (String level : com.aptisprep.mvc.enums.CefrLevel.scoringCodes())
                ranges.put(skill + "_" + level, new ScoreBandDTO());
        }
    }

    public ScoringSchemeDTO(
            Integer id,
            Integer certificateId,
            Certificate certificate,
            String name,
            String version,
            String status,
            String examType,
            String scopeSkill,
            LocalDateTime effectiveFrom,
            LocalDateTime createdAt) {
        this();
        this.id = id;
        this.certificateId = certificateId;
        this.certificate = certificate;
        this.name = name;
        this.version = version;
        this.status = status;
        this.examType = examType;
        this.scopeSkill = scopeSkill;
        this.effectiveFrom = effectiveFrom;
        this.createdAt = createdAt;
    }

    public static ScoringSchemeDTO convertEntityToDto(ScoringScheme entity) {
        if (entity == null) return null;
        return new ScoringSchemeDTO(
                entity.getId(),
                entity.getCertificate(),
                entity.getEffectiveFrom(),
                entity.getCreatedAt(),
                entity.getCertificate() == null ? null : entity.getCertificate().getId(),
                entity.getName(),
                entity.getVersion(),
                entity.getStatus() == null ? null : entity.getStatus().name(),
                entity.getExamType(),
                entity.getScopeSkill(),
                false,
                null,
                null,
                null,
                null,
                null);
    }

    public static ScoringScheme convertDtoToEntity(ScoringSchemeDTO dto) {
        if (dto == null) return null;
        return new ScoringScheme(
                dto.getId(),
                dto.getCertificate(),
                dto.getName(),
                dto.getVersion(),
                dto.getExamType(),
                dto.getScopeSkill(),
                dto.getStatus() == null ? null : RecordStatus.valueOf(dto.getStatus()),
                dto.getEffectiveFrom(),
                dto.getCreatedAt());
    }

    public boolean isConfigureScores() {
        return configureScores;
    }

    public void setConfigureScores(boolean value) {
        configureScores = value;
    }

    public ScoringScaleRuleDTO getScale() {
        return scale;
    }

    public void setScale(ScoringScaleRuleDTO value) {
        scale = value == null ? new ScoringScaleRuleDTO() : value;
    }

    public Map<String, ScoringScaleRuleDTO> getSkillScales() {
        return skillScales;
    }

    public void setSkillScales(Map<String, ScoringScaleRuleDTO> value) {
        skillScales = value == null ? new LinkedHashMap<>() : value;
    }

    public Map<String, ScoreBandDTO> getRanges() {
        return ranges;
    }

    public void setRanges(Map<String, ScoreBandDTO> value) {
        ranges = value == null ? new LinkedHashMap<>() : value;
    }

    public String getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(String value) {
        targetLevel = value;
    }

    public BigDecimal getPassingScore() {
        return passingScore;
    }

    public void setPassingScore(BigDecimal value) {
        passingScore = value;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> value) {
        skills = value == null ? new ArrayList<>() : value;
    }

    public String getExamType() {
        return examType;
    }

    public void setExamType(String value) {
        examType = value;
    }

    public String getScopeSkill() {
        return scopeSkill;
    }

    public void setScopeSkill(String value) {
        scopeSkill = value;
    }

    public Integer getCertificateId() {
        return certificateId;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getStatus() {
        return status;
    }

    public void setCertificateId(Integer value) {
        certificateId = value;
    }

    public void setName(String value) {
        name = value;
    }

    public void setVersion(String value) {
        version = value;
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

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate value) {
        this.certificate = value;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDateTime value) {
        this.effectiveFrom = value;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime value) {
        this.createdAt = value;
    }
}

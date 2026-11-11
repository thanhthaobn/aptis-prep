package com.aptisprep.dto;

import com.aptisprep.entity.ScoringScaleRule;
import com.aptisprep.entity.ScoringScheme;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class ScoringScaleRuleDTO {
    private Integer id;
    private Integer scoringSchemeId;
    private String skill;

    @NotNull
    @DecimalMin("0")
    private BigDecimal rawMin;

    @NotNull
    @DecimalMin("0.01")
    @DecimalMax("9999.99")
    private BigDecimal rawMax;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("999.99")
    private BigDecimal scaleMin;

    @NotNull
    @DecimalMin("0.01")
    @DecimalMax("999.99")
    private BigDecimal scaleMax;

    public ScoringScaleRuleDTO() {}

    public ScoringScaleRuleDTO(
            Integer id,
            Integer scoringSchemeId,
            String skill,
            BigDecimal rawMin,
            BigDecimal rawMax,
            BigDecimal scaleMin,
            BigDecimal scaleMax) {
        this.id = id;
        this.scoringSchemeId = scoringSchemeId;
        this.skill = skill;
        this.rawMin = rawMin;
        this.rawMax = rawMax;
        this.scaleMin = scaleMin;
        this.scaleMax = scaleMax;
    }

    public static ScoringScaleRuleDTO convertEntityToDto(ScoringScaleRule entity) {
        if (entity == null) return null;
        return new ScoringScaleRuleDTO(
                entity.getId(),
                entity.getScheme() == null ? null : entity.getScheme().getId(),
                entity.getSkill(),
                entity.getRawMin(),
                entity.getRawMax(),
                entity.getScaleMin(),
                entity.getScaleMax());
    }

    public ScoringScaleRule convertDtoToEntity() {
        return convertDtoToEntity(schemeReference(this.scoringSchemeId));
    }

    public ScoringScaleRule convertDtoToEntity(ScoringScheme scheme) {
        ScoringScaleRule entity =
                new ScoringScaleRule(
                        scheme, this.skill, this.rawMin, this.rawMax, this.scaleMin, this.scaleMax);
        if (this.id != null) entity.setId(this.id);
        return entity;
    }

    public static ScoringScaleRule convertDtoToEntity(
            ScoringScaleRuleDTO dto, ScoringScheme scheme) {
        return dto == null ? null : dto.convertDtoToEntity(scheme);
    }

    public static ScoringScaleRule convertDtoToEntity(
            ScoringScaleRuleDTO dto, ScoringScheme scheme, String skill) {
        if (dto == null) return null;
        dto.setSkill(skill);
        return dto.convertDtoToEntity(scheme);
    }

    public void updateEntityFromDto(ScoringScaleRule entity) {
        if (entity == null) return;
        entity.setSkill(this.skill);
        entity.setRawMin(this.rawMin);
        entity.setRawMax(this.rawMax);
        entity.setScaleMin(this.scaleMin);
        entity.setScaleMax(this.scaleMax);
    }

    private static ScoringScheme schemeReference(Integer id) {
        if (id == null) return null;
        ScoringScheme scheme = new ScoringScheme();
        scheme.setId(id);
        return scheme;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getScoringSchemeId() {
        return scoringSchemeId;
    }

    public void setScoringSchemeId(Integer scoringSchemeId) {
        this.scoringSchemeId = scoringSchemeId;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public BigDecimal getRawMin() {
        return rawMin;
    }

    public void setRawMin(BigDecimal rawMin) {
        this.rawMin = rawMin;
    }

    public BigDecimal getRawMax() {
        return rawMax;
    }

    public void setRawMax(BigDecimal rawMax) {
        this.rawMax = rawMax;
    }

    public BigDecimal getScaleMin() {
        return scaleMin;
    }

    public void setScaleMin(BigDecimal scaleMin) {
        this.scaleMin = scaleMin;
    }

    public BigDecimal getScaleMax() {
        return scaleMax;
    }

    public void setScaleMax(BigDecimal scaleMax) {
        this.scaleMax = scaleMax;
    }
}

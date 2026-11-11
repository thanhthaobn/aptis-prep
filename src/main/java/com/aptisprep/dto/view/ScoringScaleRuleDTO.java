package com.aptisprep.dto.view;

import com.aptisprep.dto.form.ScoringScaleRuleFormDTO;
import com.aptisprep.entity.ScoringScaleRule;
import com.aptisprep.entity.ScoringScheme;
import java.math.BigDecimal;

/** DTO boundary for a scoring-scale-rule entity. */
public class ScoringScaleRuleDTO {
    public ScoringScaleRuleDTO() {}

    public ScoringScaleRuleDTO(
            String skill,
            BigDecimal rawMin,
            BigDecimal rawMax,
            BigDecimal scaleMin,
            BigDecimal scaleMax,
            ScoringScheme scheme) {
        this.skill = skill;
        this.rawMin = rawMin;
        this.rawMax = rawMax;
        this.scaleMin = scaleMin;
        this.scaleMax = scaleMax;
        this.scheme = scheme;
    }

    private String skill;
    private BigDecimal rawMin;
    private BigDecimal rawMax;
    private BigDecimal scaleMin;
    private BigDecimal scaleMax;
    private ScoringScheme scheme;

    public static ScoringScaleRuleDTO fromForm(
            ScoringScaleRuleFormDTO form, ScoringScheme scheme, String skill) {
        if (form == null) return null;
        ScoringScaleRuleDTO dto = new ScoringScaleRuleDTO();
        dto.skill = skill;
        dto.rawMin = form.getRawMin();
        dto.rawMax = form.getRawMax();
        dto.scaleMin = form.getScaleMin();
        dto.scaleMax = form.getScaleMax();
        dto.scheme = scheme;
        return dto;
    }

    public static ScoringScaleRule convertDtoToEntity(ScoringScaleRuleDTO dto) {
        if (dto == null) return null;
        ScoringScaleRule entity = new ScoringScaleRule();
        entity.setScheme(dto.scheme);
        entity.setSkill(dto.skill);
        entity.setRawMin(dto.rawMin);
        entity.setRawMax(dto.rawMax);
        entity.setScaleMin(dto.scaleMin);
        entity.setScaleMax(dto.scaleMax);
        return entity;
    }

    public static ScoringScaleRule convertDtoToEntity(
            ScoringScaleRuleFormDTO form, ScoringScheme scheme, String skill) {
        return convertDtoToEntity(fromForm(form, scheme, skill));
    }

    public static ScoringScaleRuleDTO convertEntityToDto(ScoringScaleRule entity) {
        if (entity == null) return null;
        ScoringScaleRuleDTO dto = new ScoringScaleRuleDTO();
        dto.skill = entity.getSkill();
        dto.rawMin = entity.getRawMin();
        dto.rawMax = entity.getRawMax();
        dto.scaleMin = entity.getScaleMin();
        dto.scaleMax = entity.getScaleMax();
        dto.scheme = entity.getScheme();
        return dto;
    }

    public ScoringScaleRuleFormDTO toForm() {
        ScoringScaleRuleFormDTO form = new ScoringScaleRuleFormDTO();
        form.setRawMin(rawMin);
        form.setRawMax(rawMax);
        form.setScaleMin(scaleMin);
        form.setScaleMax(scaleMax);
        return form;
    }

    public String getSkill() {
        return skill;
    }

    public BigDecimal getRawMin() {
        return rawMin;
    }

    public BigDecimal getRawMax() {
        return rawMax;
    }

    public BigDecimal getScaleMin() {
        return scaleMin;
    }

    public BigDecimal getScaleMax() {
        return scaleMax;
    }

    public ScoringScheme getScheme() {
        return scheme;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public void setRawMin(BigDecimal rawMin) {
        this.rawMin = rawMin;
    }

    public void setRawMax(BigDecimal rawMax) {
        this.rawMax = rawMax;
    }

    public void setScaleMin(BigDecimal scaleMin) {
        this.scaleMin = scaleMin;
    }

    public void setScaleMax(BigDecimal scaleMax) {
        this.scaleMax = scaleMax;
    }

    public void setScheme(ScoringScheme scheme) {
        this.scheme = scheme;
    }
}

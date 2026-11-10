package com.aptisprep.dto;

import com.aptisprep.dto.form.ScoringScaleRuleForm;
import com.aptisprep.entity.ScoringScaleRule;
import com.aptisprep.entity.ScoringScheme;
import java.math.BigDecimal;

/** DTO boundary for a scoring-scale-rule entity. */
public class ScoringScaleRuleEntityDto {
    private String skill;
    private BigDecimal rawMin;
    private BigDecimal rawMax;
    private BigDecimal scaleMin;
    private BigDecimal scaleMax;
    private ScoringScheme scheme;

    public static ScoringScaleRuleEntityDto fromForm(
            ScoringScaleRuleForm form, ScoringScheme scheme, String skill) {
        if (form == null) return null;
        ScoringScaleRuleEntityDto dto = new ScoringScaleRuleEntityDto();
        dto.skill = skill;
        dto.rawMin = form.getRawMin();
        dto.rawMax = form.getRawMax();
        dto.scaleMin = form.getScaleMin();
        dto.scaleMax = form.getScaleMax();
        dto.scheme = scheme;
        return dto;
    }

    public static ScoringScaleRule convertDtoToEntity(ScoringScaleRuleEntityDto dto) {
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
            ScoringScaleRuleForm form, ScoringScheme scheme, String skill) {
        return convertDtoToEntity(fromForm(form, scheme, skill));
    }

    public static ScoringScaleRuleEntityDto convertEntityToDto(ScoringScaleRule entity) {
        if (entity == null) return null;
        ScoringScaleRuleEntityDto dto = new ScoringScaleRuleEntityDto();
        dto.skill = entity.getSkill();
        dto.rawMin = entity.getRawMin();
        dto.rawMax = entity.getRawMax();
        dto.scaleMin = entity.getScaleMin();
        dto.scaleMax = entity.getScaleMax();
        dto.scheme = entity.getScheme();
        return dto;
    }

    public ScoringScaleRuleForm toForm() {
        ScoringScaleRuleForm form = new ScoringScaleRuleForm();
        form.setRawMin(rawMin);
        form.setRawMax(rawMax);
        form.setScaleMin(scaleMin);
        form.setScaleMax(scaleMax);
        return form;
    }

    public String getSkill() { return skill; }
    public BigDecimal getRawMin() { return rawMin; }
    public BigDecimal getRawMax() { return rawMax; }
    public BigDecimal getScaleMin() { return scaleMin; }
    public BigDecimal getScaleMax() { return scaleMax; }
    public ScoringScheme getScheme() { return scheme; }
}

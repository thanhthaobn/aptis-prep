package com.aptisprep.mvc.dto;

import java.time.*;
import java.math.*;
import java.util.*;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
public class ScoringScaleRuleDTO {
    private Integer id;
    private ScoringScheme scheme;
    private String skill;
    @NotNull(message = "Giá trị raw tối thiểu không được để trống")
    @DecimalMin(value = "0", message = "Giá trị raw tối thiểu không được âm")
    private BigDecimal rawMin;

    @NotNull(message = "Giá trị raw tối đa không được để trống")
    @DecimalMin(value = "0.01", message = "Giá trị raw tối đa phải lớn hơn 0")
    @DecimalMax(value = "9999.99", message = "Giá trị raw tối đa không vượt quá 9999.99")
    private BigDecimal rawMax;

    @NotNull(message = "Giá trị scale tối thiểu không được để trống")
    @DecimalMin(value = "0", message = "Giá trị scale tối thiểu không được âm")
    @DecimalMax(value = "999.99", message = "Giá trị scale tối thiểu không vượt quá 999.99")
    private BigDecimal scaleMin;

    @NotNull(message = "Giá trị scale tối đa không được để trống")
    @DecimalMin(value = "0.01", message = "Giá trị scale tối đa phải lớn hơn 0")
    @DecimalMax(value = "999.99", message = "Giá trị scale tối đa không vượt quá 999.99")
    private BigDecimal scaleMax;

    public ScoringScaleRuleDTO() {}

    public ScoringScaleRuleDTO(
            Integer id,
            ScoringScheme scheme,
            String skill,
            BigDecimal rawMin,
            BigDecimal rawMax,
            BigDecimal scaleMin,
            BigDecimal scaleMax) {
        this.id = id;
        this.scheme = scheme;
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
            entity.getScheme(),
            entity.getSkill(),
            entity.getRawMin(),
            entity.getRawMax(),
            entity.getScaleMin(),
            entity.getScaleMax());
    }

    public static ScoringScaleRule convertDtoToEntity(ScoringScaleRuleDTO dto) {
        if (dto == null) return null;
        return new ScoringScaleRule(
            dto.getId(),
            dto.getScheme(),
            dto.getSkill(),
            dto.getRawMin(),
            dto.getRawMax(),
            dto.getScaleMin(),
            dto.getScaleMax());
    }

    public BigDecimal getRawMin() {
        return rawMin;
    }

    public void setRawMin(BigDecimal value) {
        rawMin = value;
    }

    public BigDecimal getRawMax() {
        return rawMax;
    }

    public void setRawMax(BigDecimal value) {
        rawMax = value;
    }

    public BigDecimal getScaleMin() {
        return scaleMin;
    }

    public void setScaleMin(BigDecimal value) {
        scaleMin = value;
    }

    public BigDecimal getScaleMax() {
        return scaleMax;
    }

    public void setScaleMax(BigDecimal value) {
        scaleMax = value;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer value) {
        this.id = value;
    }

    public ScoringScheme getScheme() {
        return scheme;
    }

    public void setScheme(ScoringScheme value) {
        this.scheme = value;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String value) {
        this.skill = value;
    }
}








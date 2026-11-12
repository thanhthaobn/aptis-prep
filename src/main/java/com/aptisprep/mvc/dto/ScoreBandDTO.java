package com.aptisprep.mvc.dto;

import java.time.*;
import java.math.*;
import java.util.*;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
public class ScoreBandDTO {
    private ScoringScheme scheme;
    private Integer id;

    @NotBlank(message = "Phải chọn kỹ năng")
    private String skill;

    @NotBlank(message = "Phải chọn CEFR")
    private String cefrLevel;

    @NotNull(message = "Điểm thấp nhất không được để trống")
    @DecimalMin(value = "0.00", message = "Điểm phải từ 0 đến 999.99")
    @DecimalMax(value = "999.99", message = "Điểm phải từ 0 đến 999.99")
    private BigDecimal minScore;

    @NotNull(message = "Điểm cao nhất không được để trống")
    @DecimalMin(value = "0.00", message = "Điểm phải từ 0 đến 999.99")
    @DecimalMax(value = "999.99", message = "Điểm phải từ 0 đến 999.99")
    private BigDecimal maxScore;

    public ScoreBandDTO() {}

    public ScoreBandDTO(
        String skill,
        Integer id,
        String cefrLevel,
        BigDecimal minScore,
        BigDecimal maxScore,
        ScoringScheme scheme) {
        this.skill = skill;
        this.id = id;
        this.cefrLevel = cefrLevel;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.scheme = scheme;
    }

    public static ScoreBandDTO convertEntityToDto(ScoreBand entity) {
        if (entity == null) return null;
        return new ScoreBandDTO(
            entity.getSkill(),
            entity.getId(),
            entity.getCefrLevel(),
            entity.getMinScore(),
            entity.getMaxScore(),
            entity.getScheme());
    }

    public static ScoreBand convertDtoToEntity(ScoreBandDTO dto) {
        if (dto == null) return null;
        return new ScoreBand(
            dto.getSkill(),
            dto.getId(),
            dto.getCefrLevel(),
            dto.getMinScore(),
            dto.getMaxScore(),
            dto.getScheme());
    }

    public String getSkill() {
        return skill;
    }

    public Integer getId() {
        return id;
    }

    public String getCefrLevel() {
        return cefrLevel;
    }

    public BigDecimal getMinScore() {
        return minScore;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public void setSkill(String value) {
        skill = value;
    }

    public void setId(Integer value) {
        id = value;
    }

    public void setCefrLevel(String value) {
        cefrLevel = value;
    }

    public void setMinScore(BigDecimal value) {
        minScore = value;
    }

    public void setMaxScore(BigDecimal value) {
        maxScore = value;
    }

    public ScoringScheme getScheme() {
        return scheme;
    }

    public void setScheme(ScoringScheme value) {
        this.scheme = value;
    }
}








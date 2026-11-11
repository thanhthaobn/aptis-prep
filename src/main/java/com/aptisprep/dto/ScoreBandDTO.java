package com.aptisprep.dto;

import com.aptisprep.entity.ScoreBand;
import com.aptisprep.entity.ScoringScheme;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class ScoreBandDTO {
    private Integer id;
    private Integer scoringSchemeId;

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
            Integer id,
            Integer scoringSchemeId,
            String skill,
            String cefrLevel,
            BigDecimal minScore,
            BigDecimal maxScore) {
        this.id = id;
        this.scoringSchemeId = scoringSchemeId;
        this.skill = skill;
        this.cefrLevel = cefrLevel;
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public static ScoreBandDTO convertEntityToDto(ScoreBand entity) {
        if (entity == null) return null;
        return new ScoreBandDTO(
                entity.getId(),
                entity.getScheme() == null ? null : entity.getScheme().getId(),
                entity.getSkill(),
                entity.getCefrLevel(),
                entity.getMinScore(),
                entity.getMaxScore());
    }

    public static ScoreBand convertDtoToEntity(ScoreBandDTO dto) {
        return convertDtoToEntity(dto, schemeReference(dto == null ? null : dto.scoringSchemeId));
    }

    public static ScoreBand convertDtoToEntity(ScoreBandDTO dto, ScoringScheme scheme) {
        if (dto == null) return null;
        return new ScoreBand(dto.id, scheme, dto.skill, dto.cefrLevel, dto.minScore, dto.maxScore);
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

    public String getCefrLevel() {
        return cefrLevel;
    }

    public void setCefrLevel(String cefrLevel) {
        this.cefrLevel = cefrLevel;
    }

    public BigDecimal getMinScore() {
        return minScore;
    }

    public void setMinScore(BigDecimal minScore) {
        this.minScore = minScore;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }
}

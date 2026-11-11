package com.aptisprep.dto.view;

import com.aptisprep.dto.form.ScoreBandFormDTO;
import com.aptisprep.entity.ScoreBand;
import com.aptisprep.entity.ScoringScheme;
import java.math.BigDecimal;

/** DTO boundary for a score-band entity. */
public class ScoreBandDTO {
    public ScoreBandDTO() {}

    public ScoreBandDTO(
            String skill,
            String cefrLevel,
            BigDecimal minScore,
            BigDecimal maxScore,
            ScoringScheme scheme) {
        this.skill = skill;
        this.cefrLevel = cefrLevel;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.scheme = scheme;
    }

    private String skill;
    private String cefrLevel;
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private ScoringScheme scheme;

    public static ScoreBandDTO fromForm(ScoreBandFormDTO form, ScoringScheme scheme) {
        if (form == null) return null;
        ScoreBandDTO dto = new ScoreBandDTO();
        dto.skill = form.getSkill();
        dto.cefrLevel = form.getCefrLevel();
        dto.minScore = form.getMinScore();
        dto.maxScore = form.getMaxScore();
        dto.scheme = scheme;
        return dto;
    }

    public static ScoreBand convertDtoToEntity(ScoreBandDTO dto) {
        if (dto == null) return null;
        ScoreBand entity = new ScoreBand();
        entity.setScheme(dto.scheme);
        entity.setSkill(dto.skill);
        entity.setCefrLevel(dto.cefrLevel);
        entity.setMinScore(dto.minScore);
        entity.setMaxScore(dto.maxScore);
        return entity;
    }

    public static ScoreBand convertDtoToEntity(ScoreBandFormDTO form, ScoringScheme scheme) {
        return convertDtoToEntity(fromForm(form, scheme));
    }

    public static ScoreBandDTO convertEntityToDto(ScoreBand entity) {
        if (entity == null) return null;
        ScoreBandDTO dto = new ScoreBandDTO();
        dto.skill = entity.getSkill();
        dto.cefrLevel = entity.getCefrLevel();
        dto.minScore = entity.getMinScore();
        dto.maxScore = entity.getMaxScore();
        dto.scheme = entity.getScheme();
        return dto;
    }

    public ScoreBandFormDTO toForm() {
        ScoreBandFormDTO form = new ScoreBandFormDTO();
        form.setSkill(skill);
        form.setCefrLevel(cefrLevel);
        form.setMinScore(minScore);
        form.setMaxScore(maxScore);
        return form;
    }

    public String getSkill() {
        return skill;
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

    public ScoringScheme getScheme() {
        return scheme;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public void setCefrLevel(String cefrLevel) {
        this.cefrLevel = cefrLevel;
    }

    public void setMinScore(BigDecimal minScore) {
        this.minScore = minScore;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public void setScheme(ScoringScheme scheme) {
        this.scheme = scheme;
    }
}

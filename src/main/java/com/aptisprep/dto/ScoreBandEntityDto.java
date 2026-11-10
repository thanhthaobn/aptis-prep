package com.aptisprep.dto;

import com.aptisprep.dto.form.ScoreBandForm;
import com.aptisprep.entity.ScoreBand;
import com.aptisprep.entity.ScoringScheme;
import java.math.BigDecimal;

/** DTO boundary for a score-band entity. */
public class ScoreBandEntityDto {
    private String skill;
    private String cefrLevel;
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private ScoringScheme scheme;

    public static ScoreBandEntityDto fromForm(ScoreBandForm form, ScoringScheme scheme) {
        if (form == null) return null;
        ScoreBandEntityDto dto = new ScoreBandEntityDto();
        dto.skill = form.getSkill();
        dto.cefrLevel = form.getCefrLevel();
        dto.minScore = form.getMinScore();
        dto.maxScore = form.getMaxScore();
        dto.scheme = scheme;
        return dto;
    }

    public static ScoreBand convertDtoToEntity(ScoreBandEntityDto dto) {
        if (dto == null) return null;
        ScoreBand entity = new ScoreBand();
        entity.setScheme(dto.scheme);
        entity.setSkill(dto.skill);
        entity.setCefrLevel(dto.cefrLevel);
        entity.setMinScore(dto.minScore);
        entity.setMaxScore(dto.maxScore);
        return entity;
    }

    public static ScoreBand convertDtoToEntity(ScoreBandForm form, ScoringScheme scheme) {
        return convertDtoToEntity(fromForm(form, scheme));
    }

    public static ScoreBandEntityDto convertEntityToDto(ScoreBand entity) {
        if (entity == null) return null;
        ScoreBandEntityDto dto = new ScoreBandEntityDto();
        dto.skill = entity.getSkill();
        dto.cefrLevel = entity.getCefrLevel();
        dto.minScore = entity.getMinScore();
        dto.maxScore = entity.getMaxScore();
        dto.scheme = entity.getScheme();
        return dto;
    }

    public ScoreBandForm toForm() {
        ScoreBandForm form = new ScoreBandForm();
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
}

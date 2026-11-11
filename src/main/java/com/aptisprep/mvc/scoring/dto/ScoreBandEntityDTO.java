package com.aptisprep.mvc.scoring.dto;

import com.aptisprep.mvc.scoring.entity.ScoreBand;
import com.aptisprep.mvc.scoring.entity.ScoringScheme;
import java.math.BigDecimal;

/** DTO boundary for a score-band entity. */
public class ScoreBandEntityDTO {
    private String skill;
    private String cefrLevel;
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private ScoringScheme scheme;

    public static ScoreBandEntityDTO fromForm(ScoreBandFormDTO form, ScoringScheme scheme) {
        if (form == null) return null;
        ScoreBandEntityDTO dto = new ScoreBandEntityDTO();
        dto.skill = form.getSkill();
        dto.cefrLevel = form.getCefrLevel();
        dto.minScore = form.getMinScore();
        dto.maxScore = form.getMaxScore();
        dto.scheme = scheme;
        return dto;
    }

    public static ScoreBand convertDtoToEntity(ScoreBandEntityDTO dto) {
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

    public static ScoreBandEntityDTO convertEntityToDto(ScoreBand entity) {
        if (entity == null) return null;
        ScoreBandEntityDTO dto = new ScoreBandEntityDTO();
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
}

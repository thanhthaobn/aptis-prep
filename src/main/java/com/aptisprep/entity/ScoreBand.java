package com.aptisprep.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "score_bands", uniqueConstraints = @UniqueConstraint(name = "uq_score_bands_level", columnNames = {"scoring_scheme_id", "skill", "cefr_level"}))
public class ScoreBand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scoring_scheme_id", nullable = false)
    private ScoringScheme scheme;
    @Column(nullable = false, length = 30)
    private String skill;
    @Column(name = "cefr_level", nullable = false, length = 10)
    private String cefrLevel;
    @Column(name = "min_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal minScore;
    @Column(name = "max_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxScore;

    public Integer getId() {
        return id;
    }

    public ScoringScheme getScheme() {
        return scheme;
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

    public void setScheme(ScoringScheme scheme) {
        this.scheme = scheme;
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
}

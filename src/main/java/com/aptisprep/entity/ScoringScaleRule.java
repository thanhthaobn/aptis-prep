package com.aptisprep.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
        name = "scoring_scale_rules",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_scoring_scale_rules_scheme_skill",
                        columnNames = {"scoring_scheme_id", "skill"}))
public class ScoringScaleRule {
    public ScoringScaleRule() {}

    public ScoringScaleRule(
            ScoringScheme scheme,
            String skill,
            BigDecimal rawMin,
            BigDecimal rawMax,
            BigDecimal scaleMin,
            BigDecimal scaleMax) {
        this.scheme = scheme;
        this.skill = skill;
        this.rawMin = rawMin;
        this.rawMax = rawMax;
        this.scaleMin = scaleMin;
        this.scaleMax = scaleMax;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scoring_scheme_id", nullable = false)
    private ScoringScheme scheme;

    @Column(nullable = false, length = 30)
    private String skill;

    @Column(name = "raw_min", nullable = false, precision = 6, scale = 2)
    private BigDecimal rawMin;

    @Column(name = "raw_max", nullable = false, precision = 6, scale = 2)
    private BigDecimal rawMax;

    @Column(name = "scale_min", nullable = false, precision = 5, scale = 2)
    private BigDecimal scaleMin;

    @Column(name = "scale_max", nullable = false, precision = 5, scale = 2)
    private BigDecimal scaleMax;

    public Integer getId() {
        return id;
    }

    public ScoringScheme getScheme() {
        return scheme;
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

    public void setScheme(ScoringScheme value) {
        scheme = value;
    }

    public void setSkill(String value) {
        skill = value;
    }

    public void setRawMin(BigDecimal value) {
        rawMin = value;
    }

    public void setRawMax(BigDecimal value) {
        rawMax = value;
    }

    public void setScaleMin(BigDecimal value) {
        scaleMin = value;
    }

    public void setScaleMax(BigDecimal value) {
        scaleMax = value;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}

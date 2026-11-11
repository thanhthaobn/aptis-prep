package com.aptisprep.dto.form;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Skill-specific estimated raw-to-scale policy, configured by an administrator. */
public class ScoringScaleRuleFormDTO {
    public ScoringScaleRuleFormDTO() {}

    public ScoringScaleRuleFormDTO(
            BigDecimal rawMin, BigDecimal rawMax, BigDecimal scaleMin, BigDecimal scaleMax) {
        this.rawMin = rawMin;
        this.rawMax = rawMax;
        this.scaleMin = scaleMin;
        this.scaleMax = scaleMax;
    }

    @NotNull
    @DecimalMin("0")
    private BigDecimal rawMin;

    @NotNull
    @DecimalMin("0.01")
    @DecimalMax("9999.99")
    private BigDecimal rawMax;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("999.99")
    private BigDecimal scaleMin;

    @NotNull
    @DecimalMin("0.01")
    @DecimalMax("999.99")
    private BigDecimal scaleMax;

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
}

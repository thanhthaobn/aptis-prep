package com.aptisprep.dto.form;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class ScoreBandFormDTO {
    public ScoreBandFormDTO() {}

    public ScoreBandFormDTO(
            Integer id, String skill, String cefrLevel, BigDecimal minScore, BigDecimal maxScore) {
        this.id = id;
        this.skill = skill;
        this.cefrLevel = cefrLevel;
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

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
}

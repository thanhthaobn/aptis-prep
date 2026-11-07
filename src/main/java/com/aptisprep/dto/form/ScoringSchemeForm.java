package com.aptisprep.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;

public class ScoringSchemeForm {
    @NotNull(message = "Phải chọn chứng chỉ")
    private Integer certificateId;

    @NotBlank(message = "Tên bộ chấm điểm không được để trống")
    @Size(max = 255, message = "Tên bộ chấm điểm tối đa 255 ký tự")
    private String name;

    @NotBlank(message = "Phiên bản không được để trống")
    @Size(max = 30, message = "Phiên bản tối đa 30 ký tự")
    private String version;

    @NotBlank(message = "Trạng thái không được để trống")
    private String status = "DRAFT";

    private String examType;
    private String scopeSkill;
    private boolean configureScores;
    private final ScoringScaleRuleForm scale = new ScoringScaleRuleForm();
    private final Map<String, ScoringScaleRuleForm> skillScales = new LinkedHashMap<>();
    private final Map<String, ScoreBandForm> ranges = new LinkedHashMap<>();
    private String targetLevel;
    private BigDecimal passingScore;

    public ScoringSchemeForm() {
        for (String skill : com.aptisprep.enums.Skill.codes()) {
            skillScales.put(skill, new ScoringScaleRuleForm());
            for (String level : com.aptisprep.enums.CefrLevel.codes())
                ranges.put(skill + "_" + level, new ScoreBandForm());
        }
    }

    public boolean isConfigureScores() {
        return configureScores;
    }

    public void setConfigureScores(boolean value) {
        configureScores = value;
    }

    public ScoringScaleRuleForm getScale() {
        return scale;
    }

    public Map<String, ScoringScaleRuleForm> getSkillScales() {
        return skillScales;
    }

    public Map<String, ScoreBandForm> getRanges() {
        return ranges;
    }

    public String getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(String value) {
        targetLevel = value;
    }

    public BigDecimal getPassingScore() {
        return passingScore;
    }

    public void setPassingScore(BigDecimal value) {
        passingScore = value;
    }

    public String getExamType() {
        return examType;
    }

    public void setExamType(String value) {
        examType = value;
    }

    public String getScopeSkill() {
        return scopeSkill;
    }

    public void setScopeSkill(String value) {
        scopeSkill = value;
    }

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime effectiveFrom;

    public Integer getCertificateId() {
        return certificateId;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setCertificateId(Integer value) {
        certificateId = value;
    }

    public void setName(String value) {
        name = value;
    }

    public void setVersion(String value) {
        version = value;
    }

    public void setStatus(String value) {
        status = value;
    }

    public void setEffectiveFrom(LocalDateTime value) {
        effectiveFrom = value;
    }
}

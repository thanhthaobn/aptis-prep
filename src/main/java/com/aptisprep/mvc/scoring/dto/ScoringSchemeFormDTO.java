package com.aptisprep.mvc.scoring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class ScoringSchemeFormDTO {
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
    private final ScoringScaleRuleFormDTO scale = new ScoringScaleRuleFormDTO();
    private final Map<String, ScoringScaleRuleFormDTO> skillScales = new LinkedHashMap<>();
    private final Map<String, ScoreBandFormDTO> ranges = new LinkedHashMap<>();
    private String targetLevel;
    private BigDecimal passingScore;

    public ScoringSchemeFormDTO() {
        for (String skill : com.aptisprep.mvc.common.enums.Skill.codes()) {
            skillScales.put(skill, new ScoringScaleRuleFormDTO());
            for (String level : com.aptisprep.mvc.common.enums.CefrLevel.scoringCodes())
                ranges.put(skill + "_" + level, new ScoreBandFormDTO());
        }
    }

    public boolean isConfigureScores() {
        return configureScores;
    }

    public void setConfigureScores(boolean value) {
        configureScores = value;
    }

    public ScoringScaleRuleFormDTO getScale() {
        return scale;
    }

    public Map<String, ScoringScaleRuleFormDTO> getSkillScales() {
        return skillScales;
    }

    public Map<String, ScoreBandFormDTO> getRanges() {
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
}

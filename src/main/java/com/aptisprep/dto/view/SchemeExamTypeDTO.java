package com.aptisprep.dto.view;

public class SchemeExamTypeDTO {
    private Integer schemeId;
    private String examType;

    public SchemeExamTypeDTO() {}

    public SchemeExamTypeDTO(Integer schemeId, String examType) {
        this.schemeId = schemeId;
        this.examType = examType;
    }

    public Integer getSchemeId() {
        return schemeId;
    }

    public void setSchemeId(Integer schemeId) {
        this.schemeId = schemeId;
    }

    public String getExamType() {
        return examType;
    }

    public void setExamType(String examType) {
        this.examType = examType;
    }
}

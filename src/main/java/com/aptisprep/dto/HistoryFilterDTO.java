package com.aptisprep.dto;

public class HistoryFilterDTO {
    private String query;
    private String skill;
    private String examType;
    private String state;

    public HistoryFilterDTO() {}

    public HistoryFilterDTO(String query, String skill, String examType, String state) {
        this.query = query;
        this.skill = skill;
        this.examType = examType;
        this.state = state;
    }

    public static HistoryFilterDTO empty() {
        return new HistoryFilterDTO("", "", "", "");
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String query() {
        return query;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public String skill() {
        return skill;
    }

    public String getExamType() {
        return examType;
    }

    public void setExamType(String examType) {
        this.examType = examType;
    }

    public String examType() {
        return examType;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String state() {
        return state;
    }
}

package com.aptisprep.dto;

import java.time.Instant;

public class AdminHistoryFilterDTO {
    private String query;
    private String skill;
    private String examType;
    private String state;
    private Instant submittedFrom;
    private Instant submittedTo;

    public AdminHistoryFilterDTO() {}

    public AdminHistoryFilterDTO(
            String query,
            String skill,
            String examType,
            String state,
            Instant submittedFrom,
            Instant submittedTo) {
        this.query = query;
        this.skill = skill;
        this.examType = examType;
        this.state = state;
        this.submittedFrom = submittedFrom;
        this.submittedTo = submittedTo;
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

    public Instant getSubmittedFrom() {
        return submittedFrom;
    }

    public void setSubmittedFrom(Instant submittedFrom) {
        this.submittedFrom = submittedFrom;
    }

    public Instant submittedFrom() {
        return submittedFrom;
    }

    public Instant getSubmittedTo() {
        return submittedTo;
    }

    public void setSubmittedTo(Instant submittedTo) {
        this.submittedTo = submittedTo;
    }

    public Instant submittedTo() {
        return submittedTo;
    }
}

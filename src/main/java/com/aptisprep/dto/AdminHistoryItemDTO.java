package com.aptisprep.dto;

public class AdminHistoryItemDTO {
    private String learnerName;
    private String learnerEmail;
    private AttemptResultDTO result;

    public AdminHistoryItemDTO() {}

    public AdminHistoryItemDTO(String learnerName, String learnerEmail, AttemptResultDTO result) {
        this.learnerName = learnerName;
        this.learnerEmail = learnerEmail;
        this.result = result;
    }

    public String getLearnerName() {
        return learnerName;
    }

    public void setLearnerName(String learnerName) {
        this.learnerName = learnerName;
    }

    public String learnerName() {
        return learnerName;
    }

    public String getLearnerEmail() {
        return learnerEmail;
    }

    public void setLearnerEmail(String learnerEmail) {
        this.learnerEmail = learnerEmail;
    }

    public String learnerEmail() {
        return learnerEmail;
    }

    public AttemptResultDTO getResult() {
        return result;
    }

    public void setResult(AttemptResultDTO result) {
        this.result = result;
    }

    public AttemptResultDTO result() {
        return result;
    }
}

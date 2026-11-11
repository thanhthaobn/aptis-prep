package com.aptisprep.dto;

import java.time.Instant;

public class AttemptHandleDTO {
    private Integer attemptId;
    private String state;
    private Instant deadline;

    public AttemptHandleDTO() {}

    public AttemptHandleDTO(Integer attemptId, String state, Instant deadline) {
        this.attemptId = attemptId;
        this.state = state;
        this.deadline = deadline;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public Integer attemptId() {
        return attemptId;
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

    public Instant getDeadline() {
        return deadline;
    }

    public void setDeadline(Instant deadline) {
        this.deadline = deadline;
    }

    public Instant deadline() {
        return deadline;
    }
}

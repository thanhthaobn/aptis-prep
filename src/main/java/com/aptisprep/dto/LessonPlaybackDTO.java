package com.aptisprep.dto;

public class LessonPlaybackDTO {
    private String token;
    private int sequence;
    private double duration;
    private double listened;
    private boolean verified;

    public LessonPlaybackDTO() {}

    public LessonPlaybackDTO(
            String token, int sequence, double duration, double listened, boolean verified) {
        this.token = token;
        this.sequence = sequence;
        this.duration = duration;
        this.listened = listened;
        this.verified = verified;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String token() {
        return token;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public int sequence() {
        return sequence;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public double duration() {
        return duration;
    }

    public double getListened() {
        return listened;
    }

    public void setListened(double listened) {
        this.listened = listened;
    }

    public double listened() {
        return listened;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean verified() {
        return verified;
    }
}

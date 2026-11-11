package com.aptisprep.dto;

public class LessonMediaResourceDTO {
    private String path;
    private String originalName;

    public LessonMediaResourceDTO() {}

    public LessonMediaResourceDTO(String path, String originalName) {
        this.path = path;
        this.originalName = originalName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String originalName() {
        return originalName;
    }
}

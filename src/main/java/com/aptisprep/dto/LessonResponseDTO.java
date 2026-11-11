package com.aptisprep.dto;

public class LessonResponseDTO {
    private Integer id;
    private String audioPath;
    private String responseText;

    public LessonResponseDTO() {}

    public LessonResponseDTO(Integer id, String audioPath, String responseText) {
        this.id = id;
        this.audioPath = audioPath;
        this.responseText = responseText;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer id() {
        return id;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public String audioPath() {
        return audioPath;
    }

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }

    public String responseText() {
        return responseText;
    }
}

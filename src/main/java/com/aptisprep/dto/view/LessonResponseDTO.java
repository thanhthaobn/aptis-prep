package com.aptisprep.dto.view;

public record LessonResponseDTO(Integer id, String audioPath, String responseText) {
    public String getAudioPath() {
        return audioPath;
    }

    public String getResponseText() {
        return responseText;
    }
}

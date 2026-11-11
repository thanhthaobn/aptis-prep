package com.aptisprep.presentation;

public record LessonResponseDTO(Integer id, String audioPath, String responseText) {
    public String getAudioPath() {
        return audioPath;
    }

    public String getResponseText() {
        return responseText;
    }
}

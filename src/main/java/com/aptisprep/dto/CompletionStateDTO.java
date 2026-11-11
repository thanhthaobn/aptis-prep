package com.aptisprep.dto;

public class CompletionStateDTO extends LessonTeachingViewsDTO.CompletionStateDTO {
    public CompletionStateDTO() {
        super();
    }

    public CompletionStateDTO(
            boolean hasQuestions,
            boolean checkSubmitted,
            boolean needsResponse,
            boolean responseSaved,
            boolean needsAudio,
            boolean audioPlayed,
            boolean eligible) {
        super(
                hasQuestions,
                checkSubmitted,
                needsResponse,
                responseSaved,
                needsAudio,
                audioPlayed,
                eligible);
    }
}

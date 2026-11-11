package com.aptisprep.dto;

public class ReleaseStateDTO extends LessonTeachingViewsDTO.ReleaseStateDTO {
    public ReleaseStateDTO() {
        super();
    }

    public ReleaseStateDTO(
            boolean transcript, boolean sample, String transcriptHint, String sampleHint) {
        super(transcript, sample, transcriptHint, sampleHint);
    }
}

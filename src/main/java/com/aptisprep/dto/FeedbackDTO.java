package com.aptisprep.dto;

import java.util.List;

public class FeedbackDTO extends LessonTeachingViewsDTO.FeedbackDTO {
    public FeedbackDTO() {
        super();
    }

    public FeedbackDTO(
            int score, int total, List<LessonTeachingViewsDTO.AnswerFeedbackDTO> answers) {
        super(score, total, (List) answers);
    }
}

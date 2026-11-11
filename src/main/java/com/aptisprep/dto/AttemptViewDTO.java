package com.aptisprep.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AttemptViewDTO extends LessonTeachingViewsDTO.AttemptViewDTO {
    public AttemptViewDTO() {
        super();
    }

    public AttemptViewDTO(
            Integer id,
            int score,
            int total,
            LocalDateTime submittedAt,
            List<LessonTeachingViewsDTO.AnswerFeedbackDTO> answers,
            LessonContextSnapshot context) {
        super(id, score, total, submittedAt, (List) answers, context);
    }
}

package com.aptisprep.dto;

import java.util.List;

public class AnswerFeedbackDTO extends LessonTeachingViewsDTO.AnswerFeedbackDTO {
    public AnswerFeedbackDTO() {
        super();
    }

    public AnswerFeedbackDTO(
            String prompt,
            String selected,
            String correct,
            boolean right,
            String explanation,
            List<String> options) {
        super(prompt, selected, correct, right, explanation, options);
    }
}

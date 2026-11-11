package com.aptisprep.dto;

public class StageLessonDTO extends LearningPathViewsDTO.StageLessonDTO {
    public StageLessonDTO() {
        super();
    }

    public StageLessonDTO(
            Integer id,
            String title,
            String level,
            String status,
            boolean available,
            String topic,
            boolean required,
            boolean completed) {
        super(id, title, level, status, available, topic, required, completed);
    }
}

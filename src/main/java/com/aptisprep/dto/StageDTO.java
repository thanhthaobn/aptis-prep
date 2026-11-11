package com.aptisprep.dto;

import java.util.List;

public class StageDTO extends LearningPathViewsDTO.StageDTO {
    public StageDTO() {
        super();
    }

    public StageDTO(
            Integer pathId,
            String skill,
            String fromLevel,
            String targetLevel,
            String goalLevel,
            String currentLevel,
            String status,
            List<LearningPathViewsDTO.StageLessonDTO> lessons) {
        super(
                pathId,
                skill,
                fromLevel,
                targetLevel,
                goalLevel,
                currentLevel,
                status,
                (List) lessons);
    }
}

package com.aptisprep.dto;

public class StageProgressDTO extends LearningPathViewsDTO.StageProgressDTO {
    public StageProgressDTO() {
        super();
    }

    public StageProgressDTO(String label, int percent) {
        super(label, percent);
    }
}

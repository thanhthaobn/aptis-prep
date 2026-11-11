package com.aptisprep.dto;

public class NeighborsDTO extends LessonTreeViewsDTO.NeighborsDTO {
    public NeighborsDTO() {
        super();
    }

    public NeighborsDTO(LessonDTO previous, LessonDTO next) {
        super(previous, next);
    }
}

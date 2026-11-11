package com.aptisprep.dto;

import java.util.List;

public class LevelBranchDTO extends LessonTreeViewsDTO.LevelBranchDTO {
    public LevelBranchDTO() {
        super();
    }

    public LevelBranchDTO(String level, List<LessonDTO> lessons) {
        super(level, lessons);
    }
}

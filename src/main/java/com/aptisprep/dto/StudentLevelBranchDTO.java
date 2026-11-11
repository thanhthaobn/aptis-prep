package com.aptisprep.dto;

import java.util.List;

public class StudentLevelBranchDTO extends LessonTreeViewsDTO.StudentLevelBranchDTO {
    public StudentLevelBranchDTO() {
        super();
    }

    public StudentLevelBranchDTO(
            String level, int count, List<LessonTreeViewsDTO.TopicBranchDTO> topics) {
        super(level, count, topics);
    }
}

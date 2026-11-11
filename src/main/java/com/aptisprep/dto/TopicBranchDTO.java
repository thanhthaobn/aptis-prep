package com.aptisprep.dto;

import java.util.List;

public class TopicBranchDTO extends LessonTreeViewsDTO.TopicBranchDTO {
    public TopicBranchDTO() {
        super();
    }

    public TopicBranchDTO(String name, boolean independent, List<LessonDTO> lessons) {
        super(name, independent, lessons);
    }
}

package com.aptisprep.dto;

import java.util.List;

public class SkillBranchDTO extends LessonTreeViewsDTO.SkillBranchDTO {
    public SkillBranchDTO() {
        super();
    }

    public SkillBranchDTO(String skill, List<LessonTreeViewsDTO.LevelBranchDTO> levels, int count) {
        super(skill, levels, count);
    }
}

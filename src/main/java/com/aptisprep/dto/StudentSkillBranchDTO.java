package com.aptisprep.dto;

import java.util.List;

public class StudentSkillBranchDTO extends LessonTreeViewsDTO.StudentSkillBranchDTO {
    public StudentSkillBranchDTO() {
        super();
    }

    public StudentSkillBranchDTO(
            String skill, int count, List<LessonTreeViewsDTO.StudentLevelBranchDTO> levels) {
        super(skill, count, levels);
    }
}

package com.aptisprep.dto.view;

import java.util.List;

public record LearnerContextDTO(
        Integer userId, List<SkillTargetDTO> targets, List<SkillLevelDTO> levels) {}

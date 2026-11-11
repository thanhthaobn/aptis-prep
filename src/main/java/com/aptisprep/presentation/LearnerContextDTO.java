package com.aptisprep.presentation;

import java.util.List;

public record LearnerContextDTO(
        Integer userId, List<SkillTargetDTO> targets, List<SkillLevelDTO> levels) {}

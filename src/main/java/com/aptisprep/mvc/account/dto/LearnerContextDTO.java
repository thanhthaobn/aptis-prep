package com.aptisprep.mvc.account.dto;

import java.util.List;

public record LearnerContextDTO(
        Integer userId, List<SkillTargetDTO> targets, List<SkillLevelDTO> levels) {}

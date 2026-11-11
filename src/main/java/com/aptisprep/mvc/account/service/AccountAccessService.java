package com.aptisprep.mvc.account.service;

import com.aptisprep.mvc.account.dto.LearnerContextDTO;
import com.aptisprep.mvc.auth.entity.User;

public interface AccountAccessService {
    Integer requireActiveStudent(String principalEmail);

    LearnerContextDTO context(Integer userId);

    void applyAssessment(Integer userId, Integer attemptId);

    User requireActiveStudentById(Integer userId);

    com.aptisprep.mvc.account.dto.AccountSnapshotDTO.GoalViewDTO goalView(Integer userId);

    java.util.Map<String, com.aptisprep.mvc.account.entity.UserSkillProfile> skillProfileMap(
            Integer userId);

    java.util.Map<String, String> activeGoalTargets(Integer userId);

    java.util.Map<Integer, String> goalSkillTargetsById(Integer userId);

    java.util.Map<String, String> activeGoalTargetsBySkill(Integer userId);
}

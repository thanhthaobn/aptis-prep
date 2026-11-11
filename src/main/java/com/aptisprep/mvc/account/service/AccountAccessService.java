package com.aptisprep.mvc.account.service;

import com.aptisprep.mvc.account.dto.AccountSnapshotDTO;
import com.aptisprep.mvc.account.dto.GoalSkillDTO;
import com.aptisprep.mvc.account.dto.LearnerContextDTO;
import com.aptisprep.mvc.account.dto.SkillLevelDTO;
import com.aptisprep.mvc.auth.entity.User;
import java.util.Map;

public interface AccountAccessService {
    Integer requireActiveStudent(String principalEmail);

    LearnerContextDTO context(Integer userId);

    void applyAssessment(Integer userId, Integer attemptId);

    User requireActiveStudentById(Integer userId);

    AccountSnapshotDTO.GoalViewDTO goalView(Integer userId);

    Map<String, SkillLevelDTO> skillProfileMap(Integer userId);

    Map<String, String> activeGoalTargets(Integer userId);

    Map<Integer, String> goalSkillTargetsById(Integer userId);

    Map<String, GoalSkillDTO> activeGoalSkillsBySkill(Integer userId);
}

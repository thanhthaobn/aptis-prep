package com.aptisprep.service.account;

import com.aptisprep.entity.User;
import com.aptisprep.presentation.AccountSnapshotDTO;
import com.aptisprep.presentation.GoalSkillDTO;
import com.aptisprep.presentation.LearnerContextDTO;
import com.aptisprep.presentation.SkillLevelDTO;
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

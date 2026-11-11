package com.aptisprep.service.account;

import com.aptisprep.dto.view.AccountSnapshotDTO;
import com.aptisprep.dto.view.GoalSkillDTO;
import com.aptisprep.dto.view.LearnerContextDTO;
import com.aptisprep.dto.view.SkillLevelDTO;
import com.aptisprep.entity.User;
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

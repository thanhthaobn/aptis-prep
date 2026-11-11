package com.aptisprep.service.account;

import com.aptisprep.dto.AccountSnapshotDTO;
import com.aptisprep.dto.GoalSkillDTO;
import com.aptisprep.dto.LearnerContextDTO;
import com.aptisprep.dto.SkillLevelDTO;
import com.aptisprep.entity.User;
import java.util.Map;

public interface AccountAccessService {
    public Integer requireActiveStudent(String principalEmail);

    public LearnerContextDTO context(Integer userId);

    public void applyAssessment(Integer userId, Integer attemptId);

    public User requireActiveStudentById(Integer userId);

    public AccountSnapshotDTO.GoalViewDTO goalView(Integer userId);

    public Map<String, SkillLevelDTO> skillProfileMap(Integer userId);

    public Map<String, String> activeGoalTargets(Integer userId);

    public Map<Integer, String> goalSkillTargetsById(Integer userId);

    public Map<String, GoalSkillDTO> activeGoalSkillsBySkill(Integer userId);
}

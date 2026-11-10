package com.aptisprep.mvc.account.service;

import com.aptisprep.mvc.account.dto.LearnerContextDTO;
import com.aptisprep.mvc.auth.entity.User;

public interface AccountAccessService {
    Integer requireActiveStudent(String principalEmail);

    LearnerContextDTO context(Integer userId);

    void applyAssessment(Integer userId, Integer attemptId);

    User requireActiveStudentById(Integer userId);
}

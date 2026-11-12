package com.aptisprep.mvc.service.account;

import com.aptisprep.mvc.dto.UserDTO;
import com.aptisprep.mvc.entity.User;
import com.aptisprep.mvc.support.contract.IntegrationModels.LearnerContext;

public interface AccountAccessService {
    public Integer requireActiveStudent(String principalEmail);

    public LearnerContext context(Integer userId);

    public void applyAssessment(Integer userId, Integer attemptId);

    public User requireAccount(String email);

    public default UserDTO requireAccountView(String email) {
        return UserDTO.convertEntityToDto(requireAccount(email));
    }

    public User requireActiveStudentById(Integer userId);

}








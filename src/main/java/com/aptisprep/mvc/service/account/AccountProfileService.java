package com.aptisprep.mvc.service.account;

import com.aptisprep.mvc.dto.*;
import com.aptisprep.mvc.dto.UserGoalDTO;
import com.aptisprep.mvc.entity.Certificate;
import java.util.List;

public interface AccountProfileService {
    public UserDTO profile(String principalEmail);

    public UserGoalDTO goal(String principalEmail);

    public List<UserSkillProfileDTO> skillProfiles(String principalEmail);

    public void updateProfile(String principalEmail, UserDTO form);

    public void saveGoal(String principalEmail, UserGoalDTO form);

    public List<Certificate> activeCertificates();
}

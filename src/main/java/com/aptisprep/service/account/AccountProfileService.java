package com.aptisprep.service.account;

import com.aptisprep.dto.form.GoalForm;
import com.aptisprep.dto.form.ProfileForm;
import com.aptisprep.dto.view.AccountSnapshot;
import com.aptisprep.mvc.scoring.entity.Certificate;
import java.util.List;

public interface AccountProfileService {
    AccountSnapshot snapshot(String principalEmail);

    void updateProfile(String principalEmail, ProfileForm form);

    void saveGoal(String principalEmail, GoalForm form);

    GoalForm formFrom(AccountSnapshot snapshot);

    List<Certificate> activeCertificates();
}

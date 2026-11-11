package com.aptisprep.service.account;

import com.aptisprep.entity.Certificate;
import com.aptisprep.presentation.AccountSnapshotDTO;
import com.aptisprep.presentation.form.GoalFormDTO;
import com.aptisprep.presentation.form.ProfileFormDTO;
import java.util.List;

public interface AccountProfileService {
    AccountSnapshotDTO snapshot(String principalEmail);

    void updateProfile(String principalEmail, ProfileFormDTO form);

    void saveGoal(String principalEmail, GoalFormDTO form);

    GoalFormDTO formFrom(AccountSnapshotDTO snapshot);

    List<Certificate> activeCertificates();
}

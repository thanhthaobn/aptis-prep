package com.aptisprep.service.account;

import com.aptisprep.dto.form.GoalFormDTO;
import com.aptisprep.dto.form.ProfileFormDTO;
import com.aptisprep.dto.view.AccountSnapshotDTO;
import com.aptisprep.entity.Certificate;
import java.util.List;

public interface AccountProfileService {
    AccountSnapshotDTO snapshot(String principalEmail);

    void updateProfile(String principalEmail, ProfileFormDTO form);

    void saveGoal(String principalEmail, GoalFormDTO form);

    GoalFormDTO formFrom(AccountSnapshotDTO snapshot);

    List<Certificate> activeCertificates();
}

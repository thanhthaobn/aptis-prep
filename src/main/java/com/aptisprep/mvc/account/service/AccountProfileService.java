package com.aptisprep.mvc.account.service;

import com.aptisprep.mvc.account.dto.AccountSnapshotDTO;
import com.aptisprep.mvc.account.dto.GoalFormDTO;
import com.aptisprep.mvc.account.dto.ProfileFormDTO;
import com.aptisprep.mvc.scoring.entity.Certificate;
import java.util.List;

public interface AccountProfileService {
    AccountSnapshotDTO snapshot(String principalEmail);

    void updateProfile(String principalEmail, ProfileFormDTO form);

    void saveGoal(String principalEmail, GoalFormDTO form);

    GoalFormDTO formFrom(AccountSnapshotDTO snapshot);

    List<Certificate> activeCertificates();
}

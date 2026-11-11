package com.aptisprep.service.account;

import com.aptisprep.dto.AccountSnapshotDTO;
import com.aptisprep.dto.GoalFormDTO;
import com.aptisprep.dto.ProfileFormDTO;
import com.aptisprep.entity.Certificate;
import java.util.List;

public interface AccountProfileService {
    public AccountSnapshotDTO snapshot(String principalEmail);

    public void updateProfile(String principalEmail, ProfileFormDTO form);

    public void saveGoal(String principalEmail, GoalFormDTO form);

    public GoalFormDTO formFrom(AccountSnapshotDTO snapshot);

    public List<Certificate> activeCertificates();
}

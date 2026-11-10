package com.aptisprep.service.account;

import com.aptisprep.mvc.auth.entity.User;
import com.aptisprep.service.contract.AccountAccessContract;

public interface AccountAccessService extends AccountAccessContract {
    User requireActiveStudentById(Integer userId);
}

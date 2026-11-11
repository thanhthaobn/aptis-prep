package com.aptisprep.controller.admin;

import com.aptisprep.service.account.AccountUnlockRequestService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "com.aptisprep.controller.admin")
public class AdminLayoutAdvice {

    private final AccountUnlockRequestService unlockRequestService;

    public AdminLayoutAdvice(AccountUnlockRequestService unlockRequestService) {
        this.unlockRequestService = unlockRequestService;
    }

    @ModelAttribute("pendingUnlockRequestsCount")
    public long pendingUnlockRequestsCount() {
        return unlockRequestService.countPending();
    }
}

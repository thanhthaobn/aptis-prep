package com.aptisprep.mvc.service.exam;

import com.aptisprep.mvc.entity.ExamAttempt;
import com.aptisprep.mvc.support.contract.IntegrationModels.LockedAttempt;

/** Đọc snapshot và đáp án để tạo payload chấm điểm. */
public interface ExamGradingPayloadService {
    public LockedAttempt build(ExamAttempt attempt);

    public void validate(LockedAttempt locked);
}

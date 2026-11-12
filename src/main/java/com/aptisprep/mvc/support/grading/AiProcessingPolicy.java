package com.aptisprep.mvc.support.grading;

/** Các giới hạn chung của luồng xử lý, độc lập với điểm số và dữ liệu học viên. */
public final class AiProcessingPolicy {
    public static final int MAX_ATTEMPTS = 3;
    public static final int LEASE_MINUTES = 15;
    public static final int RECOVERY_BATCH_SIZE = 50;
    public static final int CLAIM_RETRY_SECONDS = 30;

    private AiProcessingPolicy() {}
}

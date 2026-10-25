package com.aptisprep.service.grading;

/** Shared workflow limits, independent of grading scores and learner data. */
public final class AiProcessingPolicy {
    public static final int MAX_ATTEMPTS = 3;
    public static final int LEASE_MINUTES = 15;
    public static final int RECOVERY_BATCH_SIZE = 50;
    public static final int CLAIM_RETRY_SECONDS = 30;

    private AiProcessingPolicy() {}
}

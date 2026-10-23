package com.aptisprep.service.contract;

/** Shared limits for the Trâm to Thông grading boundary. */
public final class GradingTextLimits {
    private GradingTextLimits() {}

    public static final int MAX_ITEM_CHARS = 20_000;
    public static final int MAX_QUESTION_CHARS = 50_000;
    public static final int MAX_AI_TASK_CHARS = 50_000;
    public static final int MAX_AI_SUBMISSION_CHARS = 50_000;
    public static final int MAX_AI_PAYLOAD_CHARS = 100_000;

    public static int length(String value) {
        return value == null ? 0 : value.length();
    }
}

package com.aptisprep.mvc.grading.service.impl;

/** Shared limits for the grading boundary. */
public final class GradingTextLimits {
    private GradingTextLimits() {}

    public static final int MAX_ITEM_CHARS = 20_000;
    public static final int MAX_QUESTION_CHARS = 50_000;
    public static final int MAX_AI_TASK_CHARS = 50_000;
    public static final int MAX_AI_SUBMISSION_CHARS = 50_000;
    // Count task + submission, including item labels/separators, before locking the attempt.
    public static final int MAX_AI_PAYLOAD_CHARS = MAX_QUESTION_CHARS;

    public static int length(String value) {
        return value == null ? 0 : value.length();
    }
}

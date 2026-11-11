package com.aptisprep.mvc.learning.service.impl;

public final class LearningProgressCalculator {
    private LearningProgressCalculator() {}

    public static Integer percent(int completed, int total) {
        if (total < 0 || completed < 0 || completed > total)
            throw new IllegalArgumentException("Tiến độ không hợp lệ");
        return total == 0 ? null : (int) Math.floor(completed * 100.0 / total);
    }
}

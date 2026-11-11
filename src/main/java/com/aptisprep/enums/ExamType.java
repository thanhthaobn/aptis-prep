package com.aptisprep.enums;

import java.util.Arrays;
import java.util.List;

public enum ExamType {
    PRACTICE,
    FULL_MOCK,
    PLACEMENT,
    CHECKPOINT;

    public static List<String> codes() {
        return Arrays.stream(values()).map(Enum::name).toList();
    }
}

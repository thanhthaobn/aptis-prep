package com.aptisprep.mvc.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public enum ExamType {
    PLACEMENT("Kiểm tra đầu vào"),
    PRACTICE("Bài luyện tập"),
    CHECKPOINT("Kiểm tra cuối chặng"),
    FULL_MOCK("Thi thử tổng hợp");

    private final String label;

    ExamType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static List<String> codes() {
        return Arrays.stream(values()).map(Enum::name).toList();
    }

    public static Map<String, String> labels() {
        return Arrays.stream(values())
                .collect(Collectors.toUnmodifiableMap(ExamType::name, ExamType::label));
    }
}

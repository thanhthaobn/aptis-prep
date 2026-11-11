package com.aptisprep.enums;

import java.util.Arrays;
import java.util.List;

public enum ContentStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED;

    public static List<String> codes() {
        return Arrays.stream(values()).map(Enum::name).toList();
    }
}

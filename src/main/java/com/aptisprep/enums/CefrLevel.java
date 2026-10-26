package com.aptisprep.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum CefrLevel {
    A1,
    A2,
    B1,
    B2,
    C1,
    C2;

    public static List<String> codes() {
        return Arrays.stream(values()).map(Enum::name).toList();
    }

    public static List<String> curriculumLevels() {
        return Arrays.stream(values())
                .filter(level -> level.ordinal() <= C1.ordinal())
                .map(Enum::name)
                .toList();
    }

    public boolean atLeast(CefrLevel other) {
        return ordinal() >= other.ordinal();
    }

    public boolean higherThan(CefrLevel other) {
        return ordinal() > other.ordinal();
    }

    public static CefrLevel parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}

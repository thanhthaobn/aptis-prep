package com.aptisprep.enums;

public enum CefrLevel {
    A1, A2, B1, B2, C1, C2;

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
        try { return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException exception) { return null; }
    }
}

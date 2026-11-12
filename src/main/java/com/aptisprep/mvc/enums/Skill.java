package com.aptisprep.mvc.enums;

import java.util.Arrays;
import java.util.List;

public enum Skill {
    GRAMMAR_VOCABULARY,
    READING,
    LISTENING,
    WRITING,
    SPEAKING;

    public String label() {
        return switch (this) {
            case GRAMMAR_VOCABULARY -> "Grammar & Vocabulary";
            case READING -> "Reading";
            case LISTENING -> "Listening";
            case WRITING -> "Writing";
            case SPEAKING -> "Speaking";
        };
    }

    public static List<String> codes() {
        return Arrays.stream(values()).map(streamValue -> streamValue.name()).toList();
    }

    public static List<String> labels() {
        return Arrays.stream(values()).map(streamValue -> streamValue.label()).toList();
    }
}

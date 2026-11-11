package com.aptisprep.mvc.common.enums;

import java.util.Set;

/** Names match the canonical V2 document and SQL constraint. */
public enum QuestionType {
    SINGLE_CHOICE,
    MATCHING,
    ORDERING,
    WRITING_TEXT,
    SPEAKING_AUDIO;

    public boolean usesOptions() {
        return this == SINGLE_CHOICE || this == MATCHING;
    }

    public boolean supportsSkill(String skill) {
        return switch (this) {
            case WRITING_TEXT -> "WRITING".equals(skill);
            case SPEAKING_AUDIO -> "SPEAKING".equals(skill);
            default -> Set.of("GRAMMAR_VOCABULARY", "READING", "LISTENING").contains(skill);
        };
    }
}

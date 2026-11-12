package com.aptisprep.mvc;

import static org.junit.jupiter.api.Assertions.*;

import com.aptisprep.mvc.enums.CefrLevel;
import com.aptisprep.mvc.enums.Skill;
import java.util.List;
import org.junit.jupiter.api.Test;

class SharedDomainVocabularyTests {
    @Test
    void skillCodesAndLabelsPreserveDatabaseAndUiOrdering() {
        assertEquals(
                List.of("GRAMMAR_VOCABULARY", "READING", "LISTENING", "WRITING", "SPEAKING"),
                Skill.codes());
        assertEquals(
                List.of("Grammar & Vocabulary", "Reading", "Listening", "Writing", "Speaking"),
                Skill.labels());
    }

    @Test
    void curriculumStopsAtC1WithoutCorruptingTheCefrScale() {
        assertEquals(List.of("A1", "A2", "B1", "B2", "C1"), CefrLevel.curriculumLevels());
        assertEquals("C2", CefrLevel.codes().getLast());
        assertThrows(UnsupportedOperationException.class, () -> Skill.codes().add("OTHER"));
    }
}











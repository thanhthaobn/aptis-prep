package com.aptisprep;

import static org.junit.jupiter.api.Assertions.*;

import com.aptisprep.enums.CefrLevel;
import com.aptisprep.enums.Skill;
import com.aptisprep.service.learning.LessonService;
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
        assertEquals(List.of("A1", "A2", "B1", "B2", "C1"), LessonService.LEVELS);
        assertEquals(CefrLevel.curriculumLevels(), LessonService.LEVELS);
        assertEquals("C2", CefrLevel.codes().getLast());
        assertThrows(UnsupportedOperationException.class, () -> Skill.codes().add("OTHER"));
    }
}

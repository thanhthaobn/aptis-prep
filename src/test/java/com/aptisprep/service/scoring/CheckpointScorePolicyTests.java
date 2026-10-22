package com.aptisprep.service.scoring;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CheckpointScorePolicyTests {
    @Test void fullMarksOnA1ConfirmA1NotC2() {
        assertThat(CheckpointScorePolicy.confirmedLevel("CHECKPOINT", "A1", "C2")).isEqualTo("A1");
        assertThat(CheckpointScorePolicy.passed("CHECKPOINT", "A1", "A1", new BigDecimal("50"))).isTrue();
    }
    @Test void passThresholdIsSixtyPercentOfFifty() {
        assertThat(CheckpointScorePolicy.passed("CHECKPOINT", "A1", "A1", new BigDecimal("30"))).isTrue();
        assertThat(CheckpointScorePolicy.passed("CHECKPOINT", "A1", "A1", new BigDecimal("29.99"))).isFalse();
        assertThat(CheckpointScorePolicy.passed("CHECKPOINT", "A1", "A1", BigDecimal.ZERO)).isFalse();
    }
    @Test void lowLevelAndMissingResultCannotPass() {
        assertThat(CheckpointScorePolicy.passed("CHECKPOINT", "A2", "A1", new BigDecimal("40"))).isFalse();
        assertThat(CheckpointScorePolicy.passed("CHECKPOINT", "A1", null, new BigDecimal("40"))).isFalse();
        assertThat(CheckpointScorePolicy.passed("CHECKPOINT", "A1", "A1", null)).isFalse();
    }
    @Test void placementAndPracticeKeepTheirExistingMapping() {
        for (String type : java.util.List.of("PLACEMENT", "PRACTICE", "FULL_MOCK"))
            assertThat(CheckpointScorePolicy.confirmedLevel(type, "A1", "C2")).isEqualTo("C2");
    }
    @Test void checkpointCannotPromoteBeyondItsTargetOrRaiseALowerEstimate() {
        assertThat(CheckpointScorePolicy.confirmedLevel("CHECKPOINT", "B1", "C1")).isEqualTo("B1");
        assertThat(CheckpointScorePolicy.confirmedLevel("CHECKPOINT", "B1", "A2")).isEqualTo("A2");
    }
}

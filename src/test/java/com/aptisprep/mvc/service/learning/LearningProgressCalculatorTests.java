package com.aptisprep.mvc.service.learning;

import static org.assertj.core.api.Assertions.*;

import com.aptisprep.mvc.support.learning.LearningProgressCalculator;
import org.junit.jupiter.api.Test;

class LearningProgressCalculatorTests {
    @Test
    void followsRealProgressContract() {
        assertThat(LearningProgressCalculator.percent(0, 0)).isNull();
        assertThat(LearningProgressCalculator.percent(8, 12)).isEqualTo(66);
        assertThat(LearningProgressCalculator.percent(2, 3)).isEqualTo(66);
        assertThat(LearningProgressCalculator.percent(199, 200)).isEqualTo(99);
        assertThat(LearningProgressCalculator.percent(12, 12)).isEqualTo(100);
    }

    @Test
    void rejectsCorruptCounts() {
        assertThatThrownBy(() -> LearningProgressCalculator.percent(4, 3))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package com.aptisprep.service.scoring;

import java.math.BigDecimal;
import java.util.List;

/** A stage exam confirms its own level, not proficiency beyond the tested syllabus. */
public final class CheckpointScorePolicy {
    private static final List<String> LEVELS = List.of("A1", "A2", "B1", "B2", "C1", "C2");

    private CheckpointScorePolicy() {}

    public static String confirmedLevel(String type, String target, String estimated) {
        if (!"CHECKPOINT".equals(type) || rank(target) < 0 || rank(estimated) < 0) return estimated;
        return rank(estimated) > rank(target) ? target : estimated;
    }

    public static boolean passed(String type, String target, String estimated, BigDecimal score) {
        if (rank(target) < 0 || rank(estimated) < rank(target)) return false;
        return score != null && score.signum() > 0;
    }

    private static int rank(String level) {
        return level == null ? -1 : LEVELS.indexOf(level);
    }
}

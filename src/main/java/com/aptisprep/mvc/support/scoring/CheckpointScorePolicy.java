package com.aptisprep.mvc.support.scoring;

import com.aptisprep.mvc.enums.CefrLevel;
import java.math.BigDecimal;
import java.util.List;

/** Đề theo chặng chỉ xác nhận trình độ của chính chặng đó, không suy rộng vượt syllabus đã thi. */
public final class CheckpointScorePolicy {
    private static final List<String> LEVELS = CefrLevel.codes();
    private static final BigDecimal CHECKPOINT_PASS_SCORE = new BigDecimal("40");

    private CheckpointScorePolicy() {}

    public static String confirmedLevel(String type, String target, String estimated) {
        if (!"CHECKPOINT".equals(type) || rank(target) < 0 || rank(estimated) < 0) return estimated;
        return rank(estimated) > rank(target) ? target : estimated;
    }

    public static boolean passed(String type, String target, String estimated, BigDecimal score) {
        if ("CHECKPOINT".equals(type)) {
            return rank(target) >= 0
                    && rank(estimated) >= 0
                    && score != null
                    && score.compareTo(CHECKPOINT_PASS_SCORE) >= 0;
        }
        if (rank(target) < 0 || rank(estimated) < rank(target)) return false;
        return score != null && score.signum() > 0;
    }

    public static String confirmedLevel(
            String type, String target, String estimated, BigDecimal score) {
        if ("CHECKPOINT".equals(type)
                && rank(target) >= 0
                && score != null
                && score.compareTo(CHECKPOINT_PASS_SCORE) >= 0) return target;
        return confirmedLevel(type, target, estimated);
    }

    private static int rank(String level) {
        return level == null ? -1 : LEVELS.indexOf(level);
    }
}

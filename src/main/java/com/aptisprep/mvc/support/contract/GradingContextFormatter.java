package com.aptisprep.mvc.support.contract;

import com.aptisprep.mvc.support.contract.IntegrationModels.LockedAnswer;
import com.aptisprep.mvc.support.contract.IntegrationModels.LockedItem;

/** Formats only the immutable snapshot DTO for the AI boundary. */
public final class GradingContextFormatter {
    public static final String DEFAULT_RUBRIC_VERSION = "aptis-rubric-v1";
    private static final String ITEM_SEPARATOR = "\n\n";

    private GradingContextFormatter() {}

    public static String task(LockedAnswer answer) {
        StringBuilder value = new StringBuilder(blank(answer.taskContent()));
        if (answer.items() == null || answer.items().isEmpty()) return value.toString();
        value.append("\n\nYÊU CẦU THEO TỪNG MỤC:");
        for (LockedItem item : answer.items()) {
            value.append(ITEM_SEPARATOR)
                    .append("Mục ")
                    .append(item.position())
                    .append(":\n")
                    .append(blank(item.taskContent()));
        }
        return value.toString();
    }

    public static String submission(LockedAnswer answer) {
        if (answer.items() == null || answer.items().isEmpty())
            return blank(answer.lockedContent());
        StringBuilder value = new StringBuilder();
        for (LockedItem item : answer.items()) {
            if (value.length() > 0) value.append(ITEM_SEPARATOR);
            value.append("Mục ")
                    .append(item.position())
                    .append(":\n")
                    .append(
                            blank(item.lockedContent()).isBlank()
                                    ? "[Chưa trả lời]"
                                    : item.lockedContent());
        }
        return value.toString();
    }

    public static boolean hasSubmission(LockedAnswer answer) {
        if (answer.items() == null || answer.items().isEmpty())
            return !blank(answer.lockedContent()).isBlank();
        return answer.items().stream().anyMatch(item -> !blank(item.lockedContent()).isBlank());
    }

    private static String blank(String value) {
        return value == null ? "" : value;
    }
}

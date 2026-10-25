package com.aptisprep.dto.view;

import java.util.List;

/**
 * UI examples only; not learner records or entity contracts.
 */
public final class StudentCatalogView {
    private StudentCatalogView() {
    }

    public record Fact(String label, String value) {
    }

    public record Block(String title, String content, String value) {
    }

    public record Entry(int id, String title, String skill, String level, String kind,
                        String state, String status, String summary, List<Fact> facts, List<Block> blocks,
                        boolean startable, String readinessMessage, List<String> readinessChecks) {
        public Entry(int id, String title, String skill, String level, String kind,
                     String state, String status, String summary, List<Fact> facts, List<Block> blocks) {
            this(id, title, skill, level, kind, state, status, summary, facts, blocks,
                    true, "Đề đã sẵn sàng để làm bài.", List.of());
        }
    }
}

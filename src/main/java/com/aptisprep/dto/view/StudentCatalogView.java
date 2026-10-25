package com.aptisprep.dto.view;

import java.util.List;

/** Student-facing summaries of published database content. */
public final class StudentCatalogView {
    private StudentCatalogView() {}

    public record Fact(String label, String value) {}

    public record Block(String title, String content, String value) {}

    public record Entry(
            int id,
            String title,
            String skill,
            String level,
            String kind,
            String state,
            String status,
            String summary,
            List<Fact> facts,
            List<Block> blocks,
            String examType,
            String skillCode) {}
}

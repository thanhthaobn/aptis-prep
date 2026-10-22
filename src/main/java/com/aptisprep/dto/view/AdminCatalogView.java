package com.aptisprep.dto.view;

import java.util.List;

/**
 * Immutable catalogue view models assembled from persisted content.
 */
public final class AdminCatalogView {
    private AdminCatalogView() {
    }

    public record Field(String label, String value) {
    }

    public record Row(String title, String description, String value) {
    }

    public record Entry(int id, String title, String subtitle, String skill, String level,
                        String kind, String status, String statusLabel, String date,
                        List<Field> facts, List<Row> rows) {
    }

    public record CatalogPage(List<Entry> items, int page, int size, int totalItems, int totalPages) {
    }

    public record Module(String key, String title, String description, String singular,
                         String createLabel, String typeLabel, List<String> kinds, List<Entry> entries) {
        public boolean editable() {
            return !key.equals("results");
        }

        public String baseUrl() {
            return "/admin/" + key;
        }
    }
}

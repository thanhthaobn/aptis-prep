package com.aptisprep.dto.view;

import java.util.List;

public final class PlacementDTO {
    private PlacementDTO() {}

    public record PlacementExam(
            Integer id,
            String title,
            String subtitle,
            int durationMinutes,
            List<Section> sections) {}

    public record Section(String skill, int durationMinutes) {}

    public record PlacementOption(
            Integer id,
            String title,
            String subtitle,
            int durationMinutes,
            List<Section> sections,
            boolean resumable) {
        public String displayTitle() {
            return title == null ? null : title.replaceFirst("^Aptis Placement 01\\s*[–-]\\s*", "");
        }
    }
}

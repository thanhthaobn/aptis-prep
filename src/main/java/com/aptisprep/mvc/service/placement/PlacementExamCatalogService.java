package com.aptisprep.mvc.service.placement;

import java.util.List;

/** Placement exam catalog service contract. */
public interface PlacementExamCatalogService {
    public List<PlacementExam> findPublished(String skill);

    public List<PlacementExam> findPublished();

    record PlacementExam(
            Integer id,
            String title,
            String subtitle,
            int durationMinutes,
            List<Section> sections) {}

    record Section(String skill, int durationMinutes) {}
}

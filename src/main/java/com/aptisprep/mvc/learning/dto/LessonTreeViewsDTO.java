package com.aptisprep.mvc.learning.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LessonTreeViewsDTO {
    private LessonTreeViewsDTO() {}

    public record LevelBranchDTO(String level, List<LessonEntityDTO> lessons) {
        public Map<String, List<LessonEntityDTO>> topics() {
            LinkedHashMap<String, List<LessonEntityDTO>> result = new LinkedHashMap<>();
            for (LessonEntityDTO lesson : lessons) {
                String topic =
                        lesson.getMaterial() == null ? null : lesson.getMaterial().getTopic();
                if (topic == null || topic.isBlank()) topic = "Bài học độc lập";
                result.computeIfAbsent(topic, key -> new ArrayList<>()).add(lesson);
            }
            return result;
        }
    }

    public record SkillBranchDTO(String skill, List<LevelBranchDTO> levels, int count) {}

    public record TopicBranchDTO(String name, boolean independent, List<LessonEntityDTO> lessons) {}

    public record StudentLevelBranchDTO(String level, int count, List<TopicBranchDTO> topics) {}

    public record StudentSkillBranchDTO(
            String skill, int count, List<StudentLevelBranchDTO> levels) {}

    public record NeighborsDTO(LessonEntityDTO previous, LessonEntityDTO next) {}
}

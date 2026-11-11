package com.aptisprep.presentation;

import com.aptisprep.dto.LessonDTO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LessonTreeViewsDTO {
    private LessonTreeViewsDTO() {}

    public record LevelBranchDTO(String level, List<LessonDTO> lessons) {
        public Map<String, List<LessonDTO>> topics() {
            LinkedHashMap<String, List<LessonDTO>> result = new LinkedHashMap<>();
            for (LessonDTO lesson : lessons) {
                String topic = lesson.getTopic();
                if (topic == null || topic.isBlank()) topic = "Bài học độc lập";
                result.computeIfAbsent(topic, key -> new ArrayList<>()).add(lesson);
            }
            return result;
        }
    }

    public record SkillBranchDTO(String skill, List<LevelBranchDTO> levels, int count) {}

    public record TopicBranchDTO(String name, boolean independent, List<LessonDTO> lessons) {}

    public record StudentLevelBranchDTO(String level, int count, List<TopicBranchDTO> topics) {}

    public record StudentSkillBranchDTO(
            String skill, int count, List<StudentLevelBranchDTO> levels) {}

    public record NeighborsDTO(LessonDTO previous, LessonDTO next) {}
}

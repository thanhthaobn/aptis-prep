package com.aptisprep.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LessonTreeViewsDTO {
    private LessonTreeViewsDTO() {}

    public static class LevelBranchDTO {
        private String level;
        private List<LessonDTO> lessons;

        public LevelBranchDTO() {}

        public LevelBranchDTO(String level, List<LessonDTO> lessons) {
            this.level = level;
            this.lessons = lessons;
        }

        public Map<String, List<LessonDTO>> topics() {
            LinkedHashMap<String, List<LessonDTO>> result = new LinkedHashMap<>();
            if (lessons != null) {
                for (LessonDTO lesson : lessons) {
                    String topic = lesson.getTopic();
                    if (topic == null || topic.isBlank()) topic = "Bài học độc lập";
                    result.computeIfAbsent(topic, key -> new ArrayList<>()).add(lesson);
                }
            }
            return result;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String level() {
            return level;
        }

        public List<LessonDTO> getLessons() {
            return lessons;
        }

        public void setLessons(List<LessonDTO> lessons) {
            this.lessons = lessons;
        }

        public List<LessonDTO> lessons() {
            return lessons;
        }
    }

    public static class SkillBranchDTO {
        private String skill;
        private List<LevelBranchDTO> levels;
        private int count;

        public SkillBranchDTO() {}

        public SkillBranchDTO(String skill, List<LevelBranchDTO> levels, int count) {
            this.skill = skill;
            this.levels = levels;
            this.count = count;
        }

        public String getSkill() {
            return skill;
        }

        public void setSkill(String skill) {
            this.skill = skill;
        }

        public String skill() {
            return skill;
        }

        public List<LevelBranchDTO> getLevels() {
            return levels;
        }

        public void setLevels(List<LevelBranchDTO> levels) {
            this.levels = levels;
        }

        public List<LevelBranchDTO> levels() {
            return levels;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int count() {
            return count;
        }
    }

    public static class TopicBranchDTO {
        private String name;
        private boolean independent;
        private List<LessonDTO> lessons;

        public TopicBranchDTO() {}

        public TopicBranchDTO(String name, boolean independent, List<LessonDTO> lessons) {
            this.name = name;
            this.independent = independent;
            this.lessons = lessons;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String name() {
            return name;
        }

        public boolean isIndependent() {
            return independent;
        }

        public void setIndependent(boolean independent) {
            this.independent = independent;
        }

        public boolean independent() {
            return independent;
        }

        public List<LessonDTO> getLessons() {
            return lessons;
        }

        public void setLessons(List<LessonDTO> lessons) {
            this.lessons = lessons;
        }

        public List<LessonDTO> lessons() {
            return lessons;
        }
    }

    public static class StudentLevelBranchDTO {
        private String level;
        private int count;
        private List<TopicBranchDTO> topics;

        public StudentLevelBranchDTO() {}

        public StudentLevelBranchDTO(String level, int count, List<TopicBranchDTO> topics) {
            this.level = level;
            this.count = count;
            this.topics = topics;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String level() {
            return level;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int count() {
            return count;
        }

        public List<TopicBranchDTO> getTopics() {
            return topics;
        }

        public void setTopics(List<TopicBranchDTO> topics) {
            this.topics = topics;
        }

        public List<TopicBranchDTO> topics() {
            return topics;
        }
    }

    public static class StudentSkillBranchDTO {
        private String skill;
        private int count;
        private List<StudentLevelBranchDTO> levels;

        public StudentSkillBranchDTO() {}

        public StudentSkillBranchDTO(String skill, int count, List<StudentLevelBranchDTO> levels) {
            this.skill = skill;
            this.count = count;
            this.levels = levels;
        }

        public String getSkill() {
            return skill;
        }

        public void setSkill(String skill) {
            this.skill = skill;
        }

        public String skill() {
            return skill;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int count() {
            return count;
        }

        public List<StudentLevelBranchDTO> getLevels() {
            return levels;
        }

        public void setLevels(List<StudentLevelBranchDTO> levels) {
            this.levels = levels;
        }

        public List<StudentLevelBranchDTO> levels() {
            return levels;
        }
    }

    public static class NeighborsDTO {
        private LessonDTO previous;
        private LessonDTO next;

        public NeighborsDTO() {}

        public NeighborsDTO(LessonDTO previous, LessonDTO next) {
            this.previous = previous;
            this.next = next;
        }

        public LessonDTO getPrevious() {
            return previous;
        }

        public void setPrevious(LessonDTO previous) {
            this.previous = previous;
        }

        public LessonDTO previous() {
            return previous;
        }

        public LessonDTO getNext() {
            return next;
        }

        public void setNext(LessonDTO next) {
            this.next = next;
        }

        public LessonDTO next() {
            return next;
        }
    }
}

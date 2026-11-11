package com.aptisprep.dto;

import java.util.List;

public final class StudentCatalogViewDTO {
    private StudentCatalogViewDTO() {}

    public static String categoryLabel(String type) {
        return switch (type == null ? "" : type) {
            case "FULL_MOCK" -> "Thi thử tổng hợp";
            case "PLACEMENT" -> "Kiểm tra đầu vào";
            case "CHECKPOINT" -> "Kiểm tra cuối chặng";
            default -> "Luyện từng kỹ năng";
        };
    }

    public static class Fact {
        private String label;
        private String value;

        public Fact() {}

        public Fact(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public static class Block {
        private String title;
        private String content;
        private String value;

        public Block() {}

        public Block(String title, String content, String value) {
            this.title = title;
            this.content = content;
            this.value = value;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String content() {
            return content;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public static class Entry {
        private int id;
        private String title;
        private String skill;
        private String level;
        private String kind;
        private String state;
        private String status;
        private String summary;
        private List<Fact> facts;
        private List<Block> blocks;
        private String examType;
        private String skillCode;
        private boolean startable = true;
        private String readinessMessage = "Đề đã sẵn sàng để làm bài.";
        private List<String> readinessChecks = List.of();

        public Entry() {}

        public Entry(
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
                String skillCode,
                boolean startable,
                String readinessMessage,
                List<String> readinessChecks) {
            this.id = id;
            this.title = title;
            this.skill = skill;
            this.level = level;
            this.kind = kind;
            this.state = state;
            this.status = status;
            this.summary = summary;
            this.facts = facts;
            this.blocks = blocks;
            this.examType = examType;
            this.skillCode = skillCode;
            this.startable = startable;
            this.readinessMessage = readinessMessage;
            this.readinessChecks = readinessChecks;
        }

        public Entry(
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
                String skillCode) {
            this(
                    id,
                    title,
                    skill,
                    level,
                    kind,
                    state,
                    status,
                    summary,
                    facts,
                    blocks,
                    examType,
                    skillCode,
                    true,
                    "Đề đã sẵn sàng để làm bài.",
                    List.of());
        }

        public Entry(
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
                boolean startable,
                String readinessMessage,
                List<String> readinessChecks) {
            this(
                    id,
                    title,
                    skill,
                    level,
                    kind,
                    state,
                    status,
                    summary,
                    facts,
                    blocks,
                    null,
                    null,
                    startable,
                    readinessMessage,
                    readinessChecks);
        }

        public Entry(
                int id,
                String title,
                String skill,
                String level,
                String kind,
                String state,
                String status,
                String summary,
                List<Fact> facts,
                List<Block> blocks) {
            this(
                    id,
                    title,
                    skill,
                    level,
                    kind,
                    state,
                    status,
                    summary,
                    facts,
                    blocks,
                    null,
                    null,
                    true,
                    "Đề đã sẵn sàng để làm bài.",
                    List.of());
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String title() {
            return title;
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

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String level() {
            return level;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public String kind() {
            return kind;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String state() {
            return state;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String status() {
            return status;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String summary() {
            return summary;
        }

        public List<Fact> getFacts() {
            return facts;
        }

        public void setFacts(List<Fact> facts) {
            this.facts = facts;
        }

        public List<Fact> facts() {
            return facts;
        }

        public List<Block> getBlocks() {
            return blocks;
        }

        public void setBlocks(List<Block> blocks) {
            this.blocks = blocks;
        }

        public List<Block> blocks() {
            return blocks;
        }

        public String getExamType() {
            return examType;
        }

        public void setExamType(String examType) {
            this.examType = examType;
        }

        public String examType() {
            return examType;
        }

        public String getSkillCode() {
            return skillCode;
        }

        public void setSkillCode(String skillCode) {
            this.skillCode = skillCode;
        }

        public String skillCode() {
            return skillCode;
        }

        public boolean isStartable() {
            return startable;
        }

        public void setStartable(boolean startable) {
            this.startable = startable;
        }

        public boolean startable() {
            return startable;
        }

        public String getReadinessMessage() {
            return readinessMessage;
        }

        public void setReadinessMessage(String readinessMessage) {
            this.readinessMessage = readinessMessage;
        }

        public String readinessMessage() {
            return readinessMessage;
        }

        public List<String> getReadinessChecks() {
            return readinessChecks;
        }

        public void setReadinessChecks(List<String> readinessChecks) {
            this.readinessChecks = readinessChecks;
        }

        public List<String> readinessChecks() {
            return readinessChecks;
        }
    }
}

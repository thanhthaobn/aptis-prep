package com.aptisprep.dto;

import java.util.List;

public final class PlacementDTO {
    private PlacementDTO() {}

    public static class Section {
        private String skill;
        private int durationMinutes;

        public Section() {}

        public Section(String skill, int durationMinutes) {
            this.skill = skill;
            this.durationMinutes = durationMinutes;
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

        public int getDurationMinutes() {
            return durationMinutes;
        }

        public void setDurationMinutes(int durationMinutes) {
            this.durationMinutes = durationMinutes;
        }

        public int durationMinutes() {
            return durationMinutes;
        }
    }

    public static class PlacementExam {
        private Integer id;
        private String title;
        private String subtitle;
        private int durationMinutes;
        private List<Section> sections;

        public PlacementExam() {}

        public PlacementExam(
                Integer id,
                String title,
                String subtitle,
                int durationMinutes,
                List<Section> sections) {
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
            this.durationMinutes = durationMinutes;
            this.sections = sections;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer id() {
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

        public String getSubtitle() {
            return subtitle;
        }

        public void setSubtitle(String subtitle) {
            this.subtitle = subtitle;
        }

        public String subtitle() {
            return subtitle;
        }

        public int getDurationMinutes() {
            return durationMinutes;
        }

        public void setDurationMinutes(int durationMinutes) {
            this.durationMinutes = durationMinutes;
        }

        public int durationMinutes() {
            return durationMinutes;
        }

        public List<Section> getSections() {
            return sections;
        }

        public void setSections(List<Section> sections) {
            this.sections = sections;
        }

        public List<Section> sections() {
            return sections;
        }
    }

    public static class PlacementOption {
        private Integer id;
        private String title;
        private String subtitle;
        private int durationMinutes;
        private List<Section> sections;
        private boolean resumable;

        public PlacementOption() {}

        public PlacementOption(
                Integer id,
                String title,
                String subtitle,
                int durationMinutes,
                List<Section> sections,
                boolean resumable) {
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
            this.durationMinutes = durationMinutes;
            this.sections = sections;
            this.resumable = resumable;
        }

        public String displayTitle() {
            return title == null ? null : title.replaceFirst("^Aptis Placement 01\\s*[-–]\\s*", "");
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer id() {
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

        public String getSubtitle() {
            return subtitle;
        }

        public void setSubtitle(String subtitle) {
            this.subtitle = subtitle;
        }

        public String subtitle() {
            return subtitle;
        }

        public int getDurationMinutes() {
            return durationMinutes;
        }

        public void setDurationMinutes(int durationMinutes) {
            this.durationMinutes = durationMinutes;
        }

        public int durationMinutes() {
            return durationMinutes;
        }

        public List<Section> getSections() {
            return sections;
        }

        public void setSections(List<Section> sections) {
            this.sections = sections;
        }

        public List<Section> sections() {
            return sections;
        }

        public boolean isResumable() {
            return resumable;
        }

        public void setResumable(boolean resumable) {
            this.resumable = resumable;
        }

        public boolean resumable() {
            return resumable;
        }
    }
}

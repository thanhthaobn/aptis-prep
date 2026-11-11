package com.aptisprep.dto;

public final class StudentDashboardViewDTO {
    private StudentDashboardViewDTO() {}

    public static class NextAction {
        private String title;
        private String url;
        private String label;

        public NextAction() {}

        public NextAction(String title, String url, String label) {
            this.title = title;
            this.url = url;
            this.label = label;
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

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String url() {
            return url;
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
    }

    public static class SkillOverview {
        private String label;
        private String current;
        private String goal;

        public SkillOverview() {}

        public SkillOverview(String label, String current, String goal) {
            this.label = label;
            this.current = current;
            this.goal = goal;
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

        public String getCurrent() {
            return current;
        }

        public void setCurrent(String current) {
            this.current = current;
        }

        public String current() {
            return current;
        }

        public String getGoal() {
            return goal;
        }

        public void setGoal(String goal) {
            this.goal = goal;
        }

        public String goal() {
            return goal;
        }
    }
}

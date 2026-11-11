package com.aptisprep.dto;

public final class StudentDashboardDTO {
    private StudentDashboardDTO() {}

    public static class NextAction extends StudentDashboardViewDTO.NextAction {
        public NextAction() {
            super();
        }

        public NextAction(String title, String url, String label) {
            super(title, url, label);
        }
    }

    public static class SkillOverview extends StudentDashboardViewDTO.SkillOverview {
        public SkillOverview() {
            super();
        }

        public SkillOverview(String label, String current, String goal) {
            super(label, current, goal);
        }
    }
}

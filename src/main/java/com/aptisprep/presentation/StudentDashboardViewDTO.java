package com.aptisprep.presentation;

public final class StudentDashboardViewDTO {
    private StudentDashboardViewDTO() {}

    public record NextAction(String title, String url, String label) {}

    public record SkillOverview(String label, String current, String goal) {}
}

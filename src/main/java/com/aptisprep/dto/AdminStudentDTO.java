package com.aptisprep.dto;

public final class AdminStudentDTO {
    private AdminStudentDTO() {}

    public static class ListItem extends AdminStudentViewDTO.ListItem {}
    public static class Progress extends AdminStudentViewDTO.Progress {}
    public static class Skill extends AdminStudentViewDTO.Skill {}
    public static class Attempt extends AdminStudentViewDTO.Attempt {}
    public static class Detail extends AdminStudentViewDTO.Detail {}
    public static class Page extends AdminStudentViewDTO.Page {}
}

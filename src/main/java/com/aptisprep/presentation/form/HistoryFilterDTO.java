package com.aptisprep.presentation.form;

public record HistoryFilterDTO(String query, String skill, String examType, String state) {
    public static HistoryFilterDTO empty() {
        return new HistoryFilterDTO("", "", "", "");
    }
}

package com.aptisprep.dto;

public final class HistoryDTO {
    private HistoryDTO() {}

    public static class AdminFilter extends AdminHistoryFilterDTO {}

    public static class Filter extends HistoryFilterDTO {}

    public static class AdminItem extends AdminHistoryItemDTO {}

    public static class AdminPage extends AdminHistoryPageDTO {}

    public static class Page extends HistoryPageDTO {}
}

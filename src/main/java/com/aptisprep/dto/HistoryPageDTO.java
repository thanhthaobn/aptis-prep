package com.aptisprep.dto;

import java.util.List;

public class HistoryPageDTO {
    private List<AttemptResultDTO> items;
    private int page;
    private int size;
    private long totalItems;

    public HistoryPageDTO() {}

    public HistoryPageDTO(List<AttemptResultDTO> items, int page, int size, long totalItems) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.totalItems = totalItems;
    }

    public int totalPages() {
        return totalItems == 0 ? 0 : (int) ((totalItems + size - 1) / size);
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    public boolean hasNext() {
        return page + 1 < totalPages();
    }

    public List<AttemptResultDTO> getItems() {
        return items;
    }

    public void setItems(List<AttemptResultDTO> items) {
        this.items = items;
    }

    public List<AttemptResultDTO> items() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int page() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int size() {
        return size;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(long totalItems) {
        this.totalItems = totalItems;
    }

    public long totalItems() {
        return totalItems;
    }
}

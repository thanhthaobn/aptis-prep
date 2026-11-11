package com.aptisprep.dto;

import java.util.List;

public final class AdminCatalogViewDTO {
    private AdminCatalogViewDTO() {}

    public static class Field extends CatalogEntryDTO.Field {
        public Field() {
            super();
        }

        public Field(String label, String value) {
            super(label, value);
        }
    }

    public static class Row extends CatalogEntryDTO.Row {
        public Row() {
            super();
        }

        public Row(String title, String description, String value) {
            super(title, description, value);
        }
    }

    public static class Entry extends CatalogEntryDTO {
        public Entry() {
            super();
        }

        public Entry(
                int id,
                String title,
                String subtitle,
                String skill,
                String level,
                String kind,
                String status,
                String statusLabel,
                String date,
                List<? extends CatalogEntryDTO.Field> facts,
                List<? extends CatalogEntryDTO.Row> rows) {
            super(id, title, subtitle, skill, level, kind, status, statusLabel, date, facts, rows);
        }
    }

    public static class CatalogPage {
        private List<Entry> items;
        private int page;
        private int size;
        private int totalItems;
        private int totalPages;

        public CatalogPage() {}

        public CatalogPage(List<Entry> items, int page, int size, int totalItems, int totalPages) {
            this.items = items;
            this.page = page;
            this.size = size;
            this.totalItems = totalItems;
            this.totalPages = totalPages;
        }

        public List<Entry> getItems() {
            return items;
        }

        public void setItems(List<Entry> items) {
            this.items = items;
        }

        public List<Entry> items() {
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

        public int getTotalItems() {
            return totalItems;
        }

        public void setTotalItems(int totalItems) {
            this.totalItems = totalItems;
        }

        public int totalItems() {
            return totalItems;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }

        public int totalPages() {
            return totalPages;
        }
    }

    public static class Module {
        private String key;
        private String title;
        private String description;
        private String singular;
        private String createLabel;
        private String typeLabel;
        private List<String> kinds;
        private List<Entry> entries;

        public Module() {}

        public Module(
                String key,
                String title,
                String description,
                String singular,
                String createLabel,
                String typeLabel,
                List<String> kinds,
                List<Entry> entries) {
            this.key = key;
            this.title = title;
            this.description = description;
            this.singular = singular;
            this.createLabel = createLabel;
            this.typeLabel = typeLabel;
            this.kinds = kinds;
            this.entries = entries;
        }

        public boolean editable() {
            return !key.equals("results");
        }

        public String baseUrl() {
            return "/admin/" + key;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String key() {
            return key;
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

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String description() {
            return description;
        }

        public String getSingular() {
            return singular;
        }

        public void setSingular(String singular) {
            this.singular = singular;
        }

        public String singular() {
            return singular;
        }

        public String getCreateLabel() {
            return createLabel;
        }

        public void setCreateLabel(String createLabel) {
            this.createLabel = createLabel;
        }

        public String createLabel() {
            return createLabel;
        }

        public String getTypeLabel() {
            return typeLabel;
        }

        public void setTypeLabel(String typeLabel) {
            this.typeLabel = typeLabel;
        }

        public String typeLabel() {
            return typeLabel;
        }

        public List<String> getKinds() {
            return kinds;
        }

        public void setKinds(List<String> kinds) {
            this.kinds = kinds;
        }

        public List<String> kinds() {
            return kinds;
        }

        public List<Entry> getEntries() {
            return entries;
        }

        public void setEntries(List<Entry> entries) {
            this.entries = entries;
        }

        public List<Entry> entries() {
            return entries;
        }
    }
}

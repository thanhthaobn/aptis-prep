package com.aptisprep.dto;

import java.util.List;

public class CatalogEntryDTO {
    private int id;
    private String title;
    private String subtitle;
    private String skill;
    private String level;
    private String kind;
    private String status;
    private String statusLabel;
    private String date;
    private List<Field> facts;
    private List<Row> rows;

    public CatalogEntryDTO() {}

    public CatalogEntryDTO(
            int id,
            String title,
            String subtitle,
            String skill,
            String level,
            String kind,
            String status,
            String statusLabel,
            String date,
            List<? extends Field> facts,
            List<? extends Row> rows) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.skill = skill;
        this.level = level;
        this.kind = kind;
        this.status = status;
        this.statusLabel = statusLabel;
        this.date = date;
        this.facts = facts == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(facts);
        this.rows = rows == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(rows);
    }

    public static class Field {
        private String label;
        private String value;

        public Field() {}
        public Field(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String label() { return label; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String value() { return value; }
    }

    public static class Row {
        private String title;
        private String description;
        private String value;

        public Row() {}
        public Row(String title, String description, String value) {
            this.title = title;
            this.description = description;
            this.value = value;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String title() { return title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String description() { return description; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String value() { return value; }
    }

    public static class CatalogPage {
        private List<AdminCatalogViewDTO.Entry> items;
        private int page;
        private int size;
        private int totalItems;
        private int totalPages;

        public CatalogPage() {}
        public CatalogPage(List<AdminCatalogViewDTO.Entry> items, int page, int size, int totalItems, int totalPages) {
            this.items = items;
            this.page = page;
            this.size = size;
            this.totalItems = totalItems;
            this.totalPages = totalPages;
        }

        public List<AdminCatalogViewDTO.Entry> getItems() { return items; }
        public void setItems(List<AdminCatalogViewDTO.Entry> items) { this.items = items; }
        public List<AdminCatalogViewDTO.Entry> items() { return items; }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public int page() { return page; }

        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        public int size() { return size; }

        public int getTotalItems() { return totalItems; }
        public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
        public int totalItems() { return totalItems; }

        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
        public int totalPages() { return totalPages; }
    }

    public static class Module {
        private String key;
        private String title;
        private String description;
        private String singular;
        private String createLabel;
        private String typeLabel;
        private List<String> kinds;
        private List<AdminCatalogViewDTO.Entry> entries;

        public Module() {}
        public Module(
                String key,
                String title,
                String description,
                String singular,
                String createLabel,
                String typeLabel,
                List<String> kinds,
                List<AdminCatalogViewDTO.Entry> entries) {
            this.key = key;
            this.title = title;
            this.description = description;
            this.singular = singular;
            this.createLabel = createLabel;
            this.typeLabel = typeLabel;
            this.kinds = kinds;
            this.entries = entries;
        }

        public boolean editable() { return !key.equals("results"); }
        public String baseUrl() { return "/admin/" + key; }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String key() { return key; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String title() { return title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String description() { return description; }

        public String getSingular() { return singular; }
        public void setSingular(String singular) { this.singular = singular; }
        public String singular() { return singular; }

        public String getCreateLabel() { return createLabel; }
        public void setCreateLabel(String createLabel) { this.createLabel = createLabel; }
        public String createLabel() { return createLabel; }

        public String getTypeLabel() { return typeLabel; }
        public void setTypeLabel(String typeLabel) { this.typeLabel = typeLabel; }
        public String typeLabel() { return typeLabel; }

        public List<String> getKinds() { return kinds; }
        public void setKinds(List<String> kinds) { this.kinds = kinds; }
        public List<String> kinds() { return kinds; }

        public List<AdminCatalogViewDTO.Entry> getEntries() { return entries; }
        public void setEntries(List<AdminCatalogViewDTO.Entry> entries) { this.entries = entries; }
        public List<AdminCatalogViewDTO.Entry> entries() { return entries; }
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int id() { return id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String title() { return title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String subtitle() { return subtitle; }

    public String getSkill() { return skill; }
    public void setSkill(String skill) { this.skill = skill; }
    public String skill() { return skill; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String level() { return level; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String kind() { return kind; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String status() { return status; }

    public String getStatusLabel() { return statusLabel; }
    public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }
    public String statusLabel() { return statusLabel; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String date() { return date; }

    public List<Field> getFacts() { return facts; }
    public void setFacts(List<Field> facts) { this.facts = facts; }
    public List<Field> facts() { return facts; }

    public List<Row> getRows() { return rows; }
    public void setRows(List<Row> rows) { this.rows = rows; }
    public List<Row> rows() { return rows; }
}

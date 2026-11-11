package com.aptisprep.dto;

import com.aptisprep.entity.Flashcard;
import com.aptisprep.entity.FlashcardSet;

/** DTO boundary for an individual flashcard entity. */
public class FlashcardDTO {
    public FlashcardDTO() {}

    public FlashcardDTO(
            String frontContent,
            String backContent,
            String cefrLevel,
            Integer displayOrder,
            String status,
            FlashcardSet set) {
        this.frontContent = frontContent;
        this.backContent = backContent;
        this.cefrLevel = cefrLevel;
        this.displayOrder = displayOrder;
        this.status = status;
        this.set = set;
    }

    private String frontContent;
    private String backContent;
    private String cefrLevel;
    private Integer displayOrder;
    private String status;
    private FlashcardSet set;

    public static FlashcardDTO fromValues(
            String frontContent,
            String backContent,
            String cefrLevel,
            Integer displayOrder,
            FlashcardSet set) {
        FlashcardDTO dto = new FlashcardDTO();
        dto.frontContent = frontContent;
        dto.backContent = backContent;
        dto.cefrLevel = cefrLevel;
        dto.displayOrder = displayOrder;
        dto.status = "ACTIVE";
        dto.set = set;
        return dto;
    }

    public static Flashcard convertDtoToEntity(FlashcardDTO dto) {
        if (dto == null) return null;
        Flashcard entity = new Flashcard();
        entity.setFrontContent(trim(dto.frontContent));
        entity.setBackContent(trim(dto.backContent));
        entity.setCefrLevel(dto.cefrLevel);
        entity.setDisplayOrder(dto.displayOrder);
        entity.setStatus(dto.status);
        entity.setSet(dto.set);
        return entity;
    }

    public static FlashcardDTO convertEntityToDto(Flashcard entity) {
        if (entity == null) return null;
        FlashcardDTO dto = new FlashcardDTO();
        dto.frontContent = entity.getFrontContent();
        dto.backContent = entity.getBackContent();
        dto.cefrLevel = entity.getCefrLevel();
        dto.displayOrder = entity.getDisplayOrder();
        dto.status = entity.getStatus();
        dto.set = entity.getSet();
        return dto;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    public String getFrontContent() {
        return frontContent;
    }

    public String getBackContent() {
        return backContent;
    }

    public String getCefrLevel() {
        return cefrLevel;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public String getStatus() {
        return status;
    }

    public FlashcardSet getSet() {
        return set;
    }

    public void setFrontContent(String frontContent) {
        this.frontContent = frontContent;
    }

    public void setBackContent(String backContent) {
        this.backContent = backContent;
    }

    public void setCefrLevel(String cefrLevel) {
        this.cefrLevel = cefrLevel;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setSet(FlashcardSet set) {
        this.set = set;
    }
}

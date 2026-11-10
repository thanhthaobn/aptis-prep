package com.aptisprep.dto;

import com.aptisprep.entity.Flashcard;
import com.aptisprep.entity.FlashcardSet;

/** DTO boundary for an individual flashcard entity. */
public class FlashcardEntityDto {
    private String frontContent;
    private String backContent;
    private String cefrLevel;
    private Integer displayOrder;
    private String status;
    private FlashcardSet set;

    public static FlashcardEntityDto fromValues(
            String frontContent,
            String backContent,
            String cefrLevel,
            Integer displayOrder,
            FlashcardSet set) {
        FlashcardEntityDto dto = new FlashcardEntityDto();
        dto.frontContent = frontContent;
        dto.backContent = backContent;
        dto.cefrLevel = cefrLevel;
        dto.displayOrder = displayOrder;
        dto.status = "ACTIVE";
        dto.set = set;
        return dto;
    }

    public static Flashcard convertDtoToEntity(FlashcardEntityDto dto) {
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

    public static FlashcardEntityDto convertEntityToDto(Flashcard entity) {
        if (entity == null) return null;
        FlashcardEntityDto dto = new FlashcardEntityDto();
        dto.frontContent = entity.getFrontContent();
        dto.backContent = entity.getBackContent();
        dto.cefrLevel = entity.getCefrLevel();
        dto.displayOrder = entity.getDisplayOrder();
        dto.status = entity.getStatus();
        dto.set = entity.getSet();
        return dto;
    }

    private static String trim(String value) { return value == null ? null : value.trim(); }

    public String getFrontContent() { return frontContent; }
    public String getBackContent() { return backContent; }
    public String getCefrLevel() { return cefrLevel; }
    public Integer getDisplayOrder() { return displayOrder; }
    public String getStatus() { return status; }
    public FlashcardSet getSet() { return set; }
}

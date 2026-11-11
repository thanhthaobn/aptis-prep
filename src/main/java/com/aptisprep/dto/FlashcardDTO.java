package com.aptisprep.dto;

import com.aptisprep.entity.Flashcard;
import com.aptisprep.entity.FlashcardSet;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FlashcardDTO {
    private Integer id;

    @NotBlank(message = "Nội dung mặt trước không được để trống")
    @Size(max = 200, message = "Nội dung mặt trước tối đa 200 ký tự")
    private String frontContent;

    @NotBlank(message = "Nội dung mặt sau không được để trống")
    @Size(max = 1000, message = "Nội dung mặt sau tối đa 1000 ký tự")
    private String backContent;

    private String cefrLevel = "A1";
    private Integer displayOrder = 0;
    private String status = "ACTIVE";
    private FlashcardSet set;
    private Integer setId;

    public FlashcardDTO() {}

    public FlashcardDTO(
            Integer id,
            String frontContent,
            String backContent,
            String cefrLevel,
            Integer displayOrder,
            String status,
            FlashcardSet set) {
        this.id = id;
        this.frontContent = frontContent;
        this.backContent = backContent;
        this.cefrLevel = cefrLevel;
        this.displayOrder = displayOrder;
        this.status = status;
        this.set = set;
        this.setId = set == null ? null : set.getId();
    }

    public static FlashcardDTO fromValues(
            String term, String definition, String cefrLevel, int displayOrder, FlashcardSet set) {
        FlashcardDTO dto = new FlashcardDTO();
        dto.setFrontContent(term);
        dto.setBackContent(definition);
        dto.setCefrLevel(cefrLevel);
        dto.setDisplayOrder(displayOrder);
        dto.setSet(set);
        return dto;
    }

    public static FlashcardDTO convertEntityToDto(Flashcard entity) {
        if (entity == null) return null;
        FlashcardDTO dto = new FlashcardDTO();
        dto.id = entity.getId();
        dto.frontContent = entity.getFrontContent();
        dto.backContent = entity.getBackContent();
        dto.cefrLevel = entity.getCefrLevel();
        dto.displayOrder = entity.getDisplayOrder();
        dto.status = entity.getStatus();
        dto.set = entity.getSet();
        dto.setId = entity.getSet() == null ? null : entity.getSet().getId();
        return dto;
    }

    public Flashcard convertDtoToEntity() {
        Flashcard entity = new Flashcard();
        updateEntityFromDto(entity);
        if (this.id != null) {
            entity.setId(this.id);
        }
        return entity;
    }

    public static Flashcard convertDtoToEntity(FlashcardDTO dto) {
        return dto == null ? null : dto.convertDtoToEntity();
    }

    public void updateEntityFromDto(Flashcard entity) {
        if (entity == null) return;
        if (this.set != null) entity.setSet(this.set);
        if (this.status != null) entity.setStatus(this.status);
        entity.setFrontContent(trim(this.frontContent));
        entity.setBackContent(trim(this.backContent));
        if (this.cefrLevel != null) entity.setCefrLevel(this.cefrLevel);
        if (this.displayOrder != null) entity.setDisplayOrder(this.displayOrder);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    // Term and definition aliases
    public String getTerm() { return frontContent; }
    public void setTerm(String term) { this.frontContent = term; }
    public String term() { return frontContent; }

    public String getDefinition() { return backContent; }
    public void setDefinition(String definition) { this.backContent = definition; }
    public String definition() { return backContent; }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer id() { return id; }

    public String getFrontContent() { return frontContent; }
    public void setFrontContent(String frontContent) { this.frontContent = frontContent; }
    public String frontContent() { return frontContent; }

    public String getBackContent() { return backContent; }
    public void setBackContent(String backContent) { this.backContent = backContent; }
    public String backContent() { return backContent; }

    public String getCefrLevel() { return cefrLevel; }
    public void setCefrLevel(String cefrLevel) { this.cefrLevel = cefrLevel; }
    public String cefrLevel() { return cefrLevel; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public Integer displayOrder() { return displayOrder; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String status() { return status; }

    public FlashcardSet getSet() { return set; }
    public void setSet(FlashcardSet set) {
        this.set = set;
        this.setId = set == null ? null : set.getId();
    }
    public FlashcardSet set() { return set; }

    public Integer getSetId() { return setId; }
    public void setSetId(Integer setId) { this.setId = setId; }
    public Integer setId() { return setId; }
}

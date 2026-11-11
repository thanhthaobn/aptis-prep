package com.aptisprep.dto;

public class LockedItemDTO {
    private Integer itemId;
    private int position;
    private String taskContent;
    private String lockedContent;
    private Integer correctOptionId;
    private Integer correctPosition;

    public LockedItemDTO() {}

    public LockedItemDTO(
            Integer itemId,
            int position,
            String taskContent,
            String lockedContent,
            Integer correctOptionId,
            Integer correctPosition) {
        this.itemId = itemId;
        this.position = position;
        this.taskContent = taskContent;
        this.lockedContent = lockedContent;
        this.correctOptionId = correctOptionId;
        this.correctPosition = correctPosition;
    }

    public LockedItemDTO(Integer itemId, Integer correctOptionId, Integer correctPosition) {
        this(itemId, 0, null, null, correctOptionId, correctPosition);
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Integer itemId() {
        return itemId;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int position() {
        return position;
    }

    public String getTaskContent() {
        return taskContent;
    }

    public void setTaskContent(String taskContent) {
        this.taskContent = taskContent;
    }

    public String taskContent() {
        return taskContent;
    }

    public String getLockedContent() {
        return lockedContent;
    }

    public void setLockedContent(String lockedContent) {
        this.lockedContent = lockedContent;
    }

    public String lockedContent() {
        return lockedContent;
    }

    public Integer getCorrectOptionId() {
        return correctOptionId;
    }

    public void setCorrectOptionId(Integer correctOptionId) {
        this.correctOptionId = correctOptionId;
    }

    public Integer correctOptionId() {
        return correctOptionId;
    }

    public Integer getCorrectPosition() {
        return correctPosition;
    }

    public void setCorrectPosition(Integer correctPosition) {
        this.correctPosition = correctPosition;
    }

    public Integer correctPosition() {
        return correctPosition;
    }
}

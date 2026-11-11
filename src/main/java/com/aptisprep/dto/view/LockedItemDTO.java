package com.aptisprep.dto.view;

/** Backend-only item snapshot. Never serialize this record to a learner-facing view. */
public record LockedItemDTO(
        Integer itemId,
        int position,
        String taskContent,
        String lockedContent,
        Integer correctOptionId,
        Integer correctPosition) {
    public LockedItemDTO(Integer itemId, Integer correctOptionId, Integer correctPosition) {
        this(itemId, 0, null, null, correctOptionId, correctPosition);
    }
}

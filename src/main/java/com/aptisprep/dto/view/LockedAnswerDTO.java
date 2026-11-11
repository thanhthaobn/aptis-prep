package com.aptisprep.dto.view;

import java.util.List;

public record LockedAnswerDTO(
        Integer answerId,
        Integer questionId,
        String skill,
        String questionType,
        String taskContent,
        String lockedContent,
        String transcript,
        List<LockedItemDTO> items) {
    public LockedAnswerDTO(
            Integer answerId,
            Integer questionId,
            String skill,
            String questionType,
            String taskContent,
            String lockedContent,
            String transcript) {
        this(
                answerId,
                questionId,
                skill,
                questionType,
                taskContent,
                lockedContent,
                transcript,
                List.of());
    }
}

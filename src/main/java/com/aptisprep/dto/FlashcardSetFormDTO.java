package com.aptisprep.dto;

import java.util.List;

public class FlashcardSetFormDTO extends FlashcardSetDTO {
    public FlashcardSetFormDTO() {
        super();
    }

    public FlashcardSetFormDTO(String title, String description, Integer lessonId, List<Row> rows) {
        super();
        setTitle(title);
        setDescription(description);
        setLessonId(lessonId);
        setRows(rows);
    }
}

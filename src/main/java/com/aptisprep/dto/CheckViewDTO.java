package com.aptisprep.dto;

import java.util.List;

public class CheckViewDTO extends LessonTeachingViewsDTO.CheckViewDTO {
    public CheckViewDTO() {
        super();
    }

    public CheckViewDTO(
            Integer id, String prompt, List<String> options, List<Integer> optionValues) {
        super(id, prompt, options, optionValues);
    }
}

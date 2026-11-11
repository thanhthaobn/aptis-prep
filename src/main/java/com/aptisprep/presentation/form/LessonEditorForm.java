package com.aptisprep.presentation.form;

import com.aptisprep.dto.LessonCheckDTO;
import com.aptisprep.dto.LessonDTO;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/** Page form that aggregates one lesson DTO with its lesson-check DTO rows. */
public class LessonEditorForm extends LessonDTO {
    private List<@Valid LessonCheckDTO> checks = new ArrayList<>();

    public LessonEditorForm() {}

    public LessonEditorForm(List<LessonCheckDTO> checks) {
        this.checks = checks == null ? new ArrayList<>() : checks;
    }

    public List<LessonCheckDTO> getChecks() {
        return checks;
    }

    public void setChecks(List<LessonCheckDTO> checks) {
        this.checks = checks == null ? new ArrayList<>() : checks;
    }
}

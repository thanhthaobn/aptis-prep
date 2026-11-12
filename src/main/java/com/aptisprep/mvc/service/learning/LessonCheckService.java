package com.aptisprep.mvc.service.learning;

import com.aptisprep.mvc.dto.LessonCheckDTO;
import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import com.aptisprep.mvc.exception.*;
import com.aptisprep.mvc.repository.*;
import com.aptisprep.mvc.service.learning.LessonTeachingService.CheckView;
import com.aptisprep.mvc.service.learning.LessonTeachingService.Feedback;
import java.util.*;
import java.util.stream.*;
import org.springframework.transaction.annotation.Transactional;

/** Knowledge-check authoring, validation and scoring; no media/AI submission responsibility. */
@Transactional
public interface LessonCheckService {
    public boolean hasChecks(Integer lessonId);

    public List<CheckView> questions(Integer id);

    public List<LessonCheckDTO> editorQuestions(Integer id);

    public void validateChecks(List<LessonCheckDTO> forms);

    public void saveChecks(Integer id, List<LessonCheckDTO> forms);

    public Feedback grade(Integer id, Map<String, String> answers, String email);
}

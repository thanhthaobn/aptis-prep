package com.aptisprep.service.admin;

import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.presentation.AdminCatalogViewDTO.Entry;
import com.aptisprep.presentation.form.ExamFormDTO;

public interface AdminExamService {
    ExamFormDTO examForm(Integer id);

    Integer saveExam(Integer id, ExamFormDTO form);

    void publishExam(Integer id);

    void unpublishExam(Integer id);

    void normalizeSections(ExamFormDTO form);

    Entry entry(MockExamEntity exam);
}

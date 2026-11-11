package com.aptisprep.service.admin;

import com.aptisprep.dto.form.ExamFormDTO;
import com.aptisprep.dto.view.AdminCatalogViewDTO.Entry;
import com.aptisprep.entity.MockExamEntity;

public interface AdminExamService {
    ExamFormDTO examForm(Integer id);

    Integer saveExam(Integer id, ExamFormDTO form);

    void publishExam(Integer id);

    void unpublishExam(Integer id);

    void normalizeSections(ExamFormDTO form);

    Entry entry(MockExamEntity exam);
}

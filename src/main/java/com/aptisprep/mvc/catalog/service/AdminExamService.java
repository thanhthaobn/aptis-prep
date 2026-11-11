package com.aptisprep.mvc.catalog.service;

import com.aptisprep.mvc.catalog.dto.AdminCatalogViewDTO.Entry;
import com.aptisprep.mvc.catalog.dto.ExamFormDTO;
import com.aptisprep.mvc.catalog.entity.MockExamEntity;

public interface AdminExamService {
    ExamFormDTO examForm(Integer id);

    Integer saveExam(Integer id, ExamFormDTO form);

    void publishExam(Integer id);

    void unpublishExam(Integer id);

    void normalizeSections(ExamFormDTO form);

    Entry entry(MockExamEntity exam);
}

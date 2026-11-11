package com.aptisprep.service.admin;

import com.aptisprep.dto.AdminCatalogViewDTO.Entry;
import com.aptisprep.dto.ExamFormDTO;
import com.aptisprep.entity.MockExamEntity;

public interface AdminExamService {
    public ExamFormDTO examForm(Integer id);

    public Integer saveExam(Integer id, ExamFormDTO form);

    public void publishExam(Integer id);

    public void unpublishExam(Integer id);

    public void normalizeSections(ExamFormDTO form);

    public Entry entry(MockExamEntity exam);
}

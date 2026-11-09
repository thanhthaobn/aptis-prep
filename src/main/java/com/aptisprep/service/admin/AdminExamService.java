package com.aptisprep.service.admin;

import com.aptisprep.dto.form.ExamForm;
import com.aptisprep.dto.view.AdminCatalogView.Entry;
import com.aptisprep.entity.MockExamEntity;

public interface AdminExamService {
    ExamForm examForm(Integer id);
    Integer saveExam(Integer id, ExamForm form);
    void publishExam(Integer id);
    void unpublishExam(Integer id);
    void normalizeSections(ExamForm form);
    Entry entry(MockExamEntity exam);
}

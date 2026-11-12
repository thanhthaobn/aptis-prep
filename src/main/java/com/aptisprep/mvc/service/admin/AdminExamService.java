package com.aptisprep.mvc.service.admin;

import com.aptisprep.mvc.dto.MockExamDTO;
import com.aptisprep.mvc.entity.MockExam;

public interface AdminExamService {
    public MockExamDTO examForm(Integer id);

    public Integer saveExam(Integer id, MockExamDTO form);

    public void publishExam(Integer id);

    public void unpublishExam(Integer id);

    public void normalizeSections(MockExamDTO form);

    public MockExamDTO entry(MockExam exam);
}

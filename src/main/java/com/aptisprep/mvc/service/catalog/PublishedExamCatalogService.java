package com.aptisprep.mvc.service.catalog;

import com.aptisprep.mvc.dto.MockExamDTO;

/** Published exam catalog service contract. */
public interface PublishedExamCatalogService {
    public MockExamDTO publishedExam(Integer userId, Integer examId);

    public MockExamDTO checkpoint(String skill, String targetLevel);
}








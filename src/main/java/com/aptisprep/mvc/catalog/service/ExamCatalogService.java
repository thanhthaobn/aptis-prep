package com.aptisprep.mvc.catalog.service;

import com.aptisprep.service.contract.IntegrationModels.ExamSummary;
import com.aptisprep.service.contract.IntegrationModels.ExamView;

/** Owner 3. Only published, valid exams; never expose marking keys to templates. */
public interface ExamCatalogService {
    ExamView publishedExam(Integer userId, Integer examId);

    ExamSummary checkpoint(String skill, String targetLevel);
}

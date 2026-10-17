package com.aptisprep.service.contract;

import com.aptisprep.service.contract.IntegrationModels.ExamView;
import com.aptisprep.service.contract.IntegrationModels.ExamSummary;

/** Owner 3. Only published, valid exams; never expose marking keys to templates. */
public interface ExamCatalogContract {
    ExamView publishedExam(Integer userId, Integer examId);
    ExamSummary checkpoint(String skill, String targetLevel);
}

package com.aptisprep.mvc.service.exam;

import com.aptisprep.mvc.entity.MockExam;
import com.aptisprep.mvc.entity.MockExamQuestion;
import java.util.List;

/** Kiểm tra điều kiện nghiệp vụ và dữ liệu lưu trữ trước khi đề được mở. */
public interface ExamReadinessService {
    public void validate(MockExam exam, List<MockExamQuestion> links);

    public void validateForPublication(MockExam exam, List<MockExamQuestion> links);

    public void validateAudio(String path);
}

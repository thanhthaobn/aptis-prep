package com.aptisprep.service.admin.impl;

import com.aptisprep.dto.view.AdminDashboardView;
import com.aptisprep.repository.LessonRepository;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.repository.QuestionRepository;
import com.aptisprep.service.admin.AdminDashboardService;
import com.aptisprep.service.admin.StudentManagementService;
import com.aptisprep.service.contract.ExamAttemptContract;
import com.aptisprep.service.contract.IntegrationModels.AdminHistoryFilter;
import java.util.ArrayList;
import java.util.Comparator;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {
    private final StudentManagementService students;
    private final LessonRepository lessonRepository;
    private final QuestionRepository questionRepository;
    private final MockExamRepository mockExamRepository;
    private final ExamAttemptContract attempts;

    public AdminDashboardServiceImpl(
            StudentManagementService students,
            LessonRepository lessonRepository,
            QuestionRepository questionRepository,
            MockExamRepository mockExamRepository,
            ExamAttemptContract attempts) {
        this.students = students;
        this.lessonRepository = lessonRepository;
        this.questionRepository = questionRepository;
        this.mockExamRepository = mockExamRepository;
        this.attempts = attempts;
    }

    @Override
    public AdminDashboardView dashboard(String adminEmail) {
        PageRequest recent = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "updatedAt", "id"));
        ArrayList<AdminDashboardView.ContentUpdate> content = new ArrayList<>();
        lessonRepository
                .findAll(recent)
                .forEach(
                        value ->
                                content.add(
                                        new AdminDashboardView.ContentUpdate(
                                                value.getTitle(),
                                                "Bài học",
                                                "/admin/lessons/" + value.getId(),
                                                value.getUpdatedAt())));
        questionRepository
                .findAll(recent)
                .forEach(
                        value ->
                                content.add(
                                        new AdminDashboardView.ContentUpdate(
                                                value.getContent(),
                                                "Câu hỏi",
                                                "/admin/questions/" + value.getId(),
                                                value.getUpdatedAt())));
        mockExamRepository
                .findAll(recent)
                .forEach(
                        value ->
                                content.add(
                                        new AdminDashboardView.ContentUpdate(
                                                value.getTitle(),
                                                "Đề thi",
                                                "/admin/exams/" + value.getId(),
                                                value.getUpdatedAt())));

        return new AdminDashboardView(
                adminEmail,
                students.totalStudents(),
                lessonRepository.count(),
                questionRepository.count(),
                mockExamRepository.countByStatus("PUBLISHED"),
                content.stream()
                        .sorted(
                                Comparator.comparing(
                                        AdminDashboardView.ContentUpdate::updatedAt,
                                        Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(3)
                        .toList(),
                attempts.adminHistory(
                                adminEmail,
                                0,
                                3,
                                new AdminHistoryFilter("", "", "", "", null, null))
                        .items());
    }
}

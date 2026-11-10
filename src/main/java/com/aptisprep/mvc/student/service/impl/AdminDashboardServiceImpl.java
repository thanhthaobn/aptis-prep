package com.aptisprep.mvc.student.service.impl;

import com.aptisprep.mvc.catalog.repository.MockExamRepository;
import com.aptisprep.mvc.catalog.repository.QuestionRepository;
import com.aptisprep.mvc.exam.dto.AdminHistoryFilterDTO;
import com.aptisprep.mvc.exam.service.ExamAttemptService;
import com.aptisprep.mvc.learning.repository.LessonRepository;
import com.aptisprep.mvc.student.dto.AdminDashboardViewDTO;
import com.aptisprep.mvc.student.service.AdminDashboardService;
import com.aptisprep.mvc.student.service.StudentManagementService;
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
    private final ExamAttemptService attempts;

    public AdminDashboardServiceImpl(
            StudentManagementService students,
            LessonRepository lessonRepository,
            QuestionRepository questionRepository,
            MockExamRepository mockExamRepository,
            ExamAttemptService attempts) {
        this.students = students;
        this.lessonRepository = lessonRepository;
        this.questionRepository = questionRepository;
        this.mockExamRepository = mockExamRepository;
        this.attempts = attempts;
    }

    @Override
    public AdminDashboardViewDTO dashboard(String adminEmail) {
        PageRequest recent = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "updatedAt", "id"));
        ArrayList<AdminDashboardViewDTO.ContentUpdate> content = new ArrayList<>();
        lessonRepository
                .findAll(recent)
                .forEach(
                        value ->
                                content.add(
                                        new AdminDashboardViewDTO.ContentUpdate(
                                                value.getTitle(),
                                                "Bài học",
                                                "/admin/lessons/" + value.getId(),
                                                value.getUpdatedAt())));
        questionRepository
                .findAll(recent)
                .forEach(
                        value ->
                                content.add(
                                        new AdminDashboardViewDTO.ContentUpdate(
                                                value.getContent(),
                                                "Câu hỏi",
                                                "/admin/questions/" + value.getId(),
                                                value.getUpdatedAt())));
        mockExamRepository
                .findAll(recent)
                .forEach(
                        value ->
                                content.add(
                                        new AdminDashboardViewDTO.ContentUpdate(
                                                value.getTitle(),
                                                "Đề thi",
                                                "/admin/exams/" + value.getId(),
                                                value.getUpdatedAt())));

        return new AdminDashboardViewDTO(
                adminEmail,
                students.totalStudents(),
                lessonRepository.count(),
                questionRepository.count(),
                mockExamRepository.countByStatus("PUBLISHED"),
                content.stream()
                        .sorted(
                                Comparator.comparing(
                                        AdminDashboardViewDTO.ContentUpdate::updatedAt,
                                        Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(3)
                        .toList(),
                attempts.adminHistory(
                                adminEmail,
                                0,
                                3,
                                new AdminHistoryFilterDTO("", "", "", "", null, null))
                        .items());
    }
}

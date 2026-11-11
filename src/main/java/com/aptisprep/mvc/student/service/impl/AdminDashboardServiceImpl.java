package com.aptisprep.mvc.student.service.impl;

import com.aptisprep.mvc.catalog.service.CatalogService;
import com.aptisprep.mvc.exam.dto.AdminHistoryFilterDTO;
import com.aptisprep.mvc.exam.service.ExamAttemptService;
import com.aptisprep.mvc.learning.service.LessonService;
import com.aptisprep.mvc.student.dto.AdminDashboardViewDTO;
import com.aptisprep.mvc.student.service.AdminDashboardService;
import com.aptisprep.mvc.student.service.StudentManagementService;
import java.util.ArrayList;
import java.util.Comparator;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {
    private final StudentManagementService students;
    private final LessonService lessonService;
    private final CatalogService catalogService;
    private final ExamAttemptService attempts;

    public AdminDashboardServiceImpl(
            StudentManagementService students,
            LessonService lessonService,
            CatalogService catalogService,
            ExamAttemptService attempts) {
        this.students = students;
        this.lessonService = lessonService;
        this.catalogService = catalogService;
        this.attempts = attempts;
    }

    @Override
    public AdminDashboardViewDTO dashboard(String adminEmail) {
        ArrayList<AdminDashboardViewDTO.ContentUpdate> content = new ArrayList<>();
        lessonService
                .findRecentLessons(3)
                .forEach(
                        value ->
                                content.add(
                                        new AdminDashboardViewDTO.ContentUpdate(
                                                value.getTitle(),
                                                "Bài học",
                                                "/admin/lessons/" + value.getId(),
                                                value.getUpdatedAt())));
        catalogService
                .findRecentQuestions(3)
                .forEach(
                        value ->
                                content.add(
                                        new AdminDashboardViewDTO.ContentUpdate(
                                                value.getContent(),
                                                "Câu hỏi",
                                                "/admin/questions/" + value.getId(),
                                                value.getUpdatedAt())));
        catalogService
                .findRecentExams(3)
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
                lessonService.countLessons(),
                catalogService.countQuestions(),
                catalogService.countPublishedExams(),
                students.pendingUnlockRequests(),
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

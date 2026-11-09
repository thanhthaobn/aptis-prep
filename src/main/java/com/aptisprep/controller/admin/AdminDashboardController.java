package com.aptisprep.controller.admin;

import com.aptisprep.repository.LessonRepository;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.repository.QuestionRepository;
import com.aptisprep.service.admin.StudentManagementService;
import com.aptisprep.service.contract.ExamAttemptContract;
import com.aptisprep.service.contract.IntegrationModels.AdminHistoryFilter;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminDashboardController {
    private final StudentManagementService students;
    private final LessonRepository lessonRepository;
    private final QuestionRepository questionRepository;
    private final MockExamRepository mockExamRepository;
    private final ExamAttemptContract attempts;

    public AdminDashboardController(
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

    @GetMapping("/admin/dashboard")
    public String dashboard(Principal principal, Model model) {
        model.addAttribute("email", principal.getName());
        model.addAttribute("studentCount", students.totalStudents());
        model.addAttribute("lessonCount", lessonRepository.count());
        model.addAttribute("questionCount", questionRepository.count());
        model.addAttribute("publishedExamCount", mockExamRepository.countByStatus("PUBLISHED"));
        PageRequest recent = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "updatedAt", "id"));
        ArrayList<ContentUpdate> content = new ArrayList<ContentUpdate>();
        lessonRepository
                .findAll(recent)
                .forEach(
                        value ->
                                content.add(
                                        new ContentUpdate(
                                                value.getTitle(),
                                                "Bài học",
                                                "/admin/lessons/" + value.getId(),
                                                value.getUpdatedAt())));
        questionRepository
                .findAll(recent)
                .forEach(
                        value ->
                                content.add(
                                        new ContentUpdate(
                                                value.getContent(),
                                                "Câu hỏi",
                                                "/admin/questions/" + value.getId(),
                                                value.getUpdatedAt())));
        mockExamRepository
                .findAll(recent)
                .forEach(
                        value ->
                                content.add(
                                        new ContentUpdate(
                                                value.getTitle(),
                                                "Đề thi",
                                                "/admin/exams/" + value.getId(),
                                                value.getUpdatedAt())));
        model.addAttribute(
                "recentContent",
                content.stream()
                        .sorted(
                                Comparator.comparing(
                                        ContentUpdate::updatedAt,
                                        Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(3)
                        .toList());
        model.addAttribute(
                "recentAttempts",
                attempts.adminHistory(
                                principal.getName(),
                                0,
                                3,
                                new AdminHistoryFilter("", "", "", "", null, null))
                        .items());
        return "admin/dashboard";
    }

    public record ContentUpdate(String title, String kind, String url, LocalDateTime updatedAt) {}
}

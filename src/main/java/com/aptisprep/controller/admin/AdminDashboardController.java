package com.aptisprep.controller.admin;

import com.aptisprep.service.admin.StudentManagementService;
import com.aptisprep.repository.LessonRepository;
import com.aptisprep.repository.QuestionRepository;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.service.contract.ExamAttemptContract;
import com.aptisprep.service.contract.IntegrationModels.AdminHistoryFilter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {
    private final StudentManagementService students;
    private final LessonRepository lessons;
    private final QuestionRepository questions;
    private final MockExamRepository exams;
    private final ExamAttemptContract attempts;

    public AdminDashboardController(StudentManagementService students, LessonRepository lessons,
            QuestionRepository questions, MockExamRepository exams, ExamAttemptContract attempts) {
        this.students = students;
        this.lessons = lessons;
        this.questions = questions;
        this.exams = exams;
        this.attempts = attempts;
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        model.addAttribute("email", principal.getName());
        model.addAttribute("studentCount", students.totalStudents());
        model.addAttribute("lessonCount", lessons.count());
        model.addAttribute("questionCount", questions.count());
        model.addAttribute("publishedExamCount", exams.countByStatus("PUBLISHED"));
        var recent = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "updatedAt", "id"));
        var content = new ArrayList<ContentUpdate>();
        lessons.findAll(recent).forEach(value -> content.add(new ContentUpdate(value.getTitle(),
                "Bài học", "/admin/lessons/" + value.getId(), value.getUpdatedAt())));
        questions.findAll(recent).forEach(value -> content.add(new ContentUpdate(value.getContent(),
                "Câu hỏi", "/admin/questions/" + value.getId(), value.getUpdatedAt())));
        exams.findAll(recent).forEach(value -> content.add(new ContentUpdate(value.getTitle(),
                "Đề thi", "/admin/exams/" + value.getId(), value.getUpdatedAt())));
        model.addAttribute("recentContent", content.stream()
                .sorted(Comparator.comparing(ContentUpdate::updatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5).toList());
        model.addAttribute("recentAttempts", attempts.adminHistory(principal.getName(), 0, 5,
                new AdminHistoryFilter("", "", "", "", null, null)).items());
        return "admin/dashboard";
    }

    public record ContentUpdate(String title, String kind, String url, LocalDateTime updatedAt) {}
}

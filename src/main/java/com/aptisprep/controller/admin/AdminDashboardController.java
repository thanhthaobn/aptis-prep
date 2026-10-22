package com.aptisprep.controller.admin;

import com.aptisprep.repository.LessonRepository;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.repository.QuestionRepository;
import com.aptisprep.service.admin.StudentManagementService;
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

    public AdminDashboardController(StudentManagementService students, LessonRepository lessons,
            QuestionRepository questions, MockExamRepository exams) {
        this.students = students;
        this.lessons = lessons;
        this.questions = questions;
        this.exams = exams;
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        model.addAttribute("email", principal.getName());
        model.addAttribute("studentCount", students.totalStudents());
        model.addAttribute("lessonCount", lessons.count());
        model.addAttribute("questionCount", questions.count());
        model.addAttribute("publishedExamCount", exams.countByStatus("PUBLISHED"));
        return "admin/dashboard";
    }
}

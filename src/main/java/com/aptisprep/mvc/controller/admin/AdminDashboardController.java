package com.aptisprep.mvc.controller.admin;
import com.aptisprep.mvc.support.contract.*;

import com.aptisprep.mvc.service.admin.AdminDashboardService;
import com.aptisprep.mvc.service.admin.StudentManagementService;
import com.aptisprep.mvc.service.exam.ExamAttemptService;
import com.aptisprep.mvc.support.contract.IntegrationModels.AdminHistoryFilter;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminDashboardController {
    private final StudentManagementService studentManagementService;
    private final AdminDashboardService adminDashboardService;
    private final ExamAttemptService examAttemptContract;

    public AdminDashboardController(
            StudentManagementService studentManagementService,
            AdminDashboardService adminDashboardService,
            ExamAttemptService examAttemptContract) {
        this.studentManagementService = studentManagementService;
        this.adminDashboardService = adminDashboardService;
        this.examAttemptContract = examAttemptContract;
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(Principal principal, Model model) {
        model.addAttribute("email", principal.getName());
        model.addAttribute("studentCount", studentManagementService.totalStudents());
        var content = adminDashboardService.contentSummary();
        model.addAttribute("lessonCount", content.lessonCount());
        model.addAttribute("questionCount", content.questionCount());
        model.addAttribute("publishedExamCount", content.publishedExamCount());
        model.addAttribute("recentContent", content.recentContent());
        model.addAttribute(
                "recentAttempts",
                examAttemptContract.adminHistory(
                                principal.getName(),
                                0,
                                3,
                                new AdminHistoryFilter("", "", "", "", null, null))
                        .items());
        return "admin/dashboard";
    }
}








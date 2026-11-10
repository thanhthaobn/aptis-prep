package com.aptisprep.controller.admin;

import com.aptisprep.enums.UserStatus;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.service.admin.StudentManagementService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminStudentController {
    private final StudentManagementService students;

    public AdminStudentController(StudentManagementService students) {
        this.students = students;
    }

    @GetMapping("/admin/students")
    public String list(
            @RequestParam(name = "q", defaultValue = "") String q,
            @RequestParam(name = "status", defaultValue = "") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Model model) {
        UserStatus filter = parseStatus(status);
        model.addAttribute("pageData", students.list(q, filter, page, size));
        model.addAttribute("q", q == null ? "" : q.trim());
        model.addAttribute("selectedStatus", status == null ? "" : status);
        return "admin/student/list";
    }

    @GetMapping("/admin/students/{id}")
    public String detail(
            @PathVariable int id,
            @RequestParam(defaultValue = "0") int attemptPage,
            @RequestParam(defaultValue = "5") int attemptSize,
            Model model) {
        model.addAttribute("student", students.detail(id, attemptPage, attemptSize));
        return "admin/student/detail";
    }

    @PostMapping("/admin/students/{id}/lock")
    public String lock(
            @PathVariable int id,
            @RequestParam(name = "reason", defaultValue = "") String reason,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            students.lock(principal.getName(), id, reason);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã khóa tài khoản học viên. Lịch sử và tiến độ được giữ nguyên.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/students/" + id;
    }

    @PostMapping("/admin/students/{id}/unlock")
    public String unlock(
            @PathVariable int id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            students.unlock(principal.getName(), id);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Đã mở khóa tài khoản học viên.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/students/" + id;
    }

    @PostMapping("/admin/students/{id}/delete")
    public String delete(
            @PathVariable int id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            students.remove(principal.getName(), id);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Đã xóa tài khoản khỏi danh sách học viên.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/students";
    }

    private UserStatus parseStatus(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return UserStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException("Trạng thái tìm kiếm không hợp lệ");
        }
    }
}

package com.aptisprep.mvc.controller.admin;

import com.aptisprep.mvc.enums.UserStatus;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.service.account.AccountUnlockRequestService;
import com.aptisprep.mvc.service.admin.StudentManagementService;
import java.security.Principal;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminStudentController {
    private final StudentManagementService studentManagementService;
    private final AccountUnlockRequestService unlockRequestService;

    public AdminStudentController(
            StudentManagementService studentManagementService,
            AccountUnlockRequestService unlockRequestService) {
        this.studentManagementService = studentManagementService;
        this.unlockRequestService = unlockRequestService;
    }

    @GetMapping("/admin/students")
    public String list(
            @RequestParam(name = "q", defaultValue = "") String q,
            @RequestParam(name = "status", defaultValue = "") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Model model) {
        UserStatus filter = null;
        if (status != null && !status.isBlank()) {
            try {
                filter = UserStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException exception) {
                throw new BusinessRuleException("Trạng thái tìm kiếm không hợp lệ");
            }
        }
        var pageData = studentManagementService.list(q, filter, page, size);
        model.addAttribute("pageData", pageData);
        model.addAttribute(
                "targetLabels",
                studentManagementService.targetLabels(
                        pageData.getContent().stream().map(value -> value.getId()).toList()));
        model.addAttribute("q", q == null ? "" : q.trim());
        model.addAttribute("selectedStatus", status == null ? "" : status);
        model.addAttribute("pendingUnlockCount", unlockRequestService.countPending());
        return "admin/student/list";
    }

    @GetMapping("/admin/students/{id}")
    public String detail(
            @PathVariable int id,
            @RequestParam(defaultValue = "0") int attemptPage,
            @RequestParam(defaultValue = "5") int attemptSize,
            Model model) {
        var student = studentManagementService.detail(id);
        var goal = studentManagementService.goal(id);
        var profiles = studentManagementService.skills(id);
        var attemptHistory = studentManagementService.attempts(id, attemptPage, attemptSize);
        model.addAttribute("student", student);
        model.addAttribute("goal", goal);
        model.addAttribute("profiles", profiles);
        model.addAttribute(
                "skillCodes",
                List.of("GRAMMAR_VOCABULARY", "READING", "LISTENING", "WRITING", "SPEAKING"));
        model.addAttribute("progress", studentManagementService.progress(id));
        model.addAttribute("attempts", attemptHistory.items());
        model.addAttribute("attemptPage", attemptHistory.page());
        model.addAttribute("attemptSize", attemptHistory.size());
        model.addAttribute("attemptTotal", attemptHistory.totalItems());
        return "admin/student/detail";
    }

    @PostMapping("/admin/students/{id}/lock")
    public String lock(
            @PathVariable int id,
            @RequestParam(name = "reason", defaultValue = "") String reason,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            studentManagementService.lock(principal.getName(), id, reason);
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
            studentManagementService.unlock(principal.getName(), id);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Đã mở khóa tài khoản học viên.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/students/" + id;
    }

    @PostMapping("/admin/students/{id}/delete")
    public String delete(
            @PathVariable int id,
            @RequestParam(name = "reason", defaultValue = "") String reason,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            studentManagementService.remove(principal.getName(), id, reason);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Đã xóa tài khoản khỏi danh sách học viên.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/students";
    }

    @PostMapping("/admin/students/{id}/restore")
    public String restore(
            @PathVariable int id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            studentManagementService.restore(principal.getName(), id);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Đã khôi phục tài khoản học viên.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/students/" + id;
    }

    @GetMapping("/admin/unlock-requests")
    public String unlockRequests(
            @RequestParam(name = "status", defaultValue = "PENDING") String status, Model model) {
        model.addAttribute("requests", unlockRequestService.list(status));
        model.addAttribute("selectedStatus", status);
        model.addAttribute("pendingCount", unlockRequestService.countPending());
        return "admin/unlock-requests/list";
    }

    @PostMapping("/admin/unlock-requests/{id}/approve")
    public String approveUnlockRequest(
            @PathVariable int id,
            @RequestParam(name = "adminNote", defaultValue = "") String adminNote,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            unlockRequestService.approve(principal.getName(), id, adminNote);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Đã duyệt yêu cầu mở khóa tài khoản.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/unlock-requests";
    }

    @PostMapping("/admin/unlock-requests/{id}/reject")
    public String rejectUnlockRequest(
            @PathVariable int id,
            @RequestParam(name = "adminNote", defaultValue = "") String adminNote,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            unlockRequestService.reject(principal.getName(), id, adminNote);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Đã từ chối yêu cầu mở khóa tài khoản.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/unlock-requests";
    }
}

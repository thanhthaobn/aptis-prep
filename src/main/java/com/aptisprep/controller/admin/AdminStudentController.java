package com.aptisprep.controller.admin;

import com.aptisprep.enums.UserStatus;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.service.admin.StudentPreviewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Routes render the read-only preview; lock/unlock POST routes are intentionally absent. */
@Controller
@RequestMapping("/admin/students")
public class AdminStudentController {
    private final StudentPreviewService preview;
    public AdminStudentController(StudentPreviewService preview) { this.preview = preview; }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "") String status, Model model) {
        if (q.length() > 255) throw new BusinessRuleException("Từ khóa tìm kiếm tối đa 255 ký tự");
        UserStatus filter = null;
        if (!status.isBlank()) {
            try { filter = UserStatus.valueOf(status); }
            catch (IllegalArgumentException exception) { throw new BusinessRuleException("Trạng thái tìm kiếm không hợp lệ"); }
        }
        model.addAttribute("students", preview.find(q, filter));
        model.addAttribute("q", q.trim());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("totalStudents", preview.total());
        model.addAttribute("activeStudents", preview.activeCount());
        model.addAttribute("lockedStudents", preview.lockedCount());
        return "admin/student/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable int id, Model model) {
        model.addAttribute("student", preview.get(id));
        return "admin/student/detail";
    }
}

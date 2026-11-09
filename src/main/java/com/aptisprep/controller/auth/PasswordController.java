package com.aptisprep.controller.auth;

import com.aptisprep.dto.form.PasswordForm;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.security.LoginSession;
import com.aptisprep.service.auth.PasswordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Objects;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PasswordController {
    private final PasswordService passwords;

    public PasswordController(PasswordService passwords) {
        this.passwords = passwords;
    }

    @GetMapping("/student/password")
    public String form(Model model) {
        if (!model.containsAttribute("passwordForm"))
            model.addAttribute("passwordForm", new PasswordForm());
        return "auth/password";
    }

    @PostMapping("/student/password")
    public String save(
            @Valid @ModelAttribute PasswordForm passwordForm,
            BindingResult errors,
            Principal principal,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirect) {
        if (!Objects.equals(passwordForm.getPassword(), passwordForm.getConfirmPassword()))
            errors.rejectValue(
                    "confirmPassword", "password.mismatch", "Mật khẩu xác nhận không khớp");
        if (passwordForm.getCurrentPassword() == null
                || passwordForm.getCurrentPassword().isBlank())
            errors.rejectValue("currentPassword", "password.required", "Nhập mật khẩu hiện tại");
        if (!errors.hasErrors()) {
            try {
                passwords.change(
                        principal.getName(),
                        passwordForm.getCurrentPassword(),
                        passwordForm.getPassword());
                LoginSession.end(request);
                redirect.addFlashAttribute(
                        "successMessage", "Đã đổi mật khẩu. Vui lòng đăng nhập lại.");
                return "redirect:/login";
            } catch (BusinessRuleException exception) {
                errors.reject("password.failed", exception.getMessage());
            }
        }
        return form(model);
    }
}

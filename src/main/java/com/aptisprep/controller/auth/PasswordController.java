package com.aptisprep.controller.auth;

import com.aptisprep.dto.form.PasswordForm;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.security.LoginSession;
import com.aptisprep.service.auth.PasswordService;
import com.aptisprep.validation.AccountInput;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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

    public static class RecoveryForm {
        @NotBlank(message = "Vui lòng nhập email")
        @Email(message = "Email không hợp lệ")
        @Size(max = 255)
        private String email;

        public String getEmail() {
            return email;
        }

        public void setEmail(String value) {
            email = AccountInput.email(value);
        }
    }

    @GetMapping("/forgot-password")
    public String forgot(Model model) {
        model.addAttribute("recoveryAvailable", passwords.recoveryAvailable());
        if (!model.containsAttribute("recoveryForm"))
            model.addAttribute("recoveryForm", new RecoveryForm());
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotSubmit(
            @Valid @ModelAttribute RecoveryForm recoveryForm, BindingResult errors, Model model) {
        if (!errors.hasErrors()) {
            try {
                passwords.requestReset(recoveryForm.getEmail());
                model.addAttribute("requested", true);
            } catch (BusinessRuleException exception) {
                errors.reject("recovery.unavailable", exception.getMessage());
            }
        }
        return forgot(model);
    }

    @GetMapping({"/reset-password", "/student/password"})
    public String form(
               @RequestParam(name = "token", defaultValue = "") String token,
            HttpServletRequest request,
            Model model) {
        model.addAttribute(
                "changing",
                request.getRequestURI().startsWith(request.getContextPath() + "/student/"));
        model.addAttribute("token", token);
        if (!model.containsAttribute("passwordForm"))
            model.addAttribute("passwordForm", new PasswordForm());
        return "auth/password";
    }

    @PostMapping({"/reset-password", "/student/password"})
    public String save(
            @Valid @ModelAttribute PasswordForm passwordForm,
            BindingResult errors,
               @RequestParam(name = "token", defaultValue = "") String token,
            Principal principal,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirect) {
        boolean changing =
                request.getRequestURI().startsWith(request.getContextPath() + "/student/");
        if (!Objects.equals(passwordForm.getPassword(), passwordForm.getConfirmPassword()))
            errors.rejectValue(
                    "confirmPassword", "password.mismatch", "Mật khẩu xác nhận không khớp");
        if (changing
                && (passwordForm.getCurrentPassword() == null
                        || passwordForm.getCurrentPassword().isBlank()))
            errors.rejectValue("currentPassword", "password.required", "Nhập mật khẩu hiện tại");
        if (!errors.hasErrors()) {
            try {
                if (changing)
                    passwords.change(
                            principal.getName(),
                            passwordForm.getCurrentPassword(),
                            passwordForm.getPassword());
                else passwords.reset(token, passwordForm.getPassword());
                LoginSession.end(request);
                redirect.addFlashAttribute(
                        "successMessage", "Đã đổi mật khẩu. Vui lòng đăng nhập lại.");
                return "redirect:/login";
            } catch (BusinessRuleException exception) {
                errors.reject("password.failed", exception.getMessage());
            }
        }
        return form(token, request, model);
    }
}

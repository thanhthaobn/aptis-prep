package com.aptisprep.mvc.controller.auth;

import com.aptisprep.mvc.dto.*;
import com.aptisprep.mvc.enums.Role;
import com.aptisprep.mvc.enums.UserStatus;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.exception.DuplicateEmailException;
import com.aptisprep.mvc.security.LoginSession;
import com.aptisprep.mvc.service.account.AccountUnlockRequestService;
import com.aptisprep.mvc.service.auth.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;
    private final AccountUnlockRequestService unlockRequestService;

    public AuthController(UserService userService) {
        this(userService, null);
    }

    @Autowired
    public AuthController(
            UserService userService, AccountUnlockRequestService unlockRequestService) {
        this.userService = userService;
        this.unlockRequestService = unlockRequestService;
    }

    @GetMapping("/login")
    public String login(Principal principal) {
        if (principal != null) {
            return "redirect:/";
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam(name = "email", defaultValue = "") String email,
            @RequestParam(name = "password", defaultValue = "") String password,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        UserDTO account = userService.authenticate(email, password);
        if (account == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Email hoặc mật khẩu không đúng.");
            return "redirect:/login";
        }
        UserStatus accountStatus =
                account.getStatus() == null ? null : UserStatus.valueOf(account.getStatus());
        if (accountStatus == UserStatus.LOCKED) {
            String reason = account.getLockReason();
            redirectAttributes.addFlashAttribute(
                    "lockReason",
                    reason == null || reason.isBlank()
                            ? "Tài khoản đã bị khóa bởi quản trị viên."
                            : reason);
            redirectAttributes.addFlashAttribute("email", account.getEmail());
            redirectAttributes.addFlashAttribute("accountStatus", UserStatus.LOCKED.name());
            return "redirect:/account-locked";
        }
        if (accountStatus != UserStatus.ACTIVE) {
            String reason = account.getLockReason();
            redirectAttributes.addFlashAttribute(
                    "lockReason",
                    reason == null || reason.isBlank()
                            ? "Tài khoản đã được xóa khỏi hệ thống."
                            : reason);
            redirectAttributes.addFlashAttribute("email", account.getEmail());
            redirectAttributes.addFlashAttribute("accountStatus", accountStatus.name());
            return "redirect:/account-locked";
        }
        LoginSession.start(request, account);
        return account.getRole() == Role.ADMIN
                ? "redirect:/admin/dashboard"
                : "redirect:/student/dashboard";
    }

    @GetMapping("/account-locked")
    public String accountLocked(
            @RequestParam(name = "email", required = false) String email, Model model) {
        String resolvedEmail = email;
        if (resolvedEmail == null || resolvedEmail.isBlank()) {
            Object flashedEmail = model.asMap().get("email");
            resolvedEmail = flashedEmail == null ? null : flashedEmail.toString();
        }
        if (resolvedEmail != null && !resolvedEmail.isBlank()) {
            model.addAttribute("email", resolvedEmail.trim());
            if (unlockRequestService != null) {
                unlockRequestService
                        .findLatestByEmail(resolvedEmail)
                        .ifPresent(value -> model.addAttribute("latestRequest", value));
            }
        }
        if (!model.containsAttribute("accountStatus"))
            model.addAttribute("accountStatus", UserStatus.LOCKED.name());
        return "auth/account-locked";
    }

    @PostMapping("/account-unlock-request")
    public String submitUnlockRequest(
            @RequestParam(name = "email", defaultValue = "") String email,
            @RequestParam(name = "password", defaultValue = "") String password,
            @RequestParam(name = "message", defaultValue = "") String message,
            RedirectAttributes redirectAttributes) {
        try {
            if (unlockRequestService == null)
                throw new BusinessRuleException("Chức năng mở khóa chưa sẵn sàng.");
            var request = unlockRequestService.submit(email, password, message);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Yêu cầu mở khóa đã được gửi thành công.");
            redirectAttributes.addFlashAttribute("accountStatus", request.getUserStatus());
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        redirectAttributes.addFlashAttribute("email", email == null ? "" : email.trim());
        return "redirect:/account-locked";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        LoginSession.end(request);
        redirectAttributes.addFlashAttribute("successMessage", "Bạn đã đăng xuất.");
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new UserDTO());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerForm") UserDTO form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (form.getEmail() == null || form.getEmail().isBlank()) {
            bindingResult.rejectValue("email", "email.required", "Email không được để trống");
        }
        if (form.getPassword() == null || form.getPassword().isBlank()) {
            bindingResult.rejectValue(
                    "password", "password.required", "Mật khẩu không được để trống");
        }
        if (form.getConfirmPassword() == null || form.getConfirmPassword().isBlank()) {
            bindingResult.rejectValue(
                    "confirmPassword",
                    "confirmPassword.required",
                    "Xác nhận mật khẩu không được để trống");
        }
        if (!Objects.equals(form.getPassword(), form.getConfirmPassword())) {
            bindingResult.rejectValue(
                    "confirmPassword", "password.mismatch", "Mật khẩu xác nhận không khớp");
        }
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.registerStudent(form);
        } catch (DuplicateEmailException exception) {
            bindingResult.rejectValue("email", "email.duplicate", exception.getMessage());
            return "auth/register";
        } catch (DataIntegrityViolationException exception) {
            bindingResult.rejectValue(
                    "email",
                    "email.duplicate",
                    "Không thể tạo tài khoản với email này. Email có thể vừa được đăng ký; hãy thử đăng nhập.");
            return "auth/register";
        } catch (BusinessRuleException exception) {
            bindingResult.reject("registration.failed", exception.getMessage());
            return "auth/register";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage", "Đăng ký thành công. Vui lòng đăng nhập.");
        return "redirect:/login";
    }
}

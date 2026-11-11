package com.aptisprep.controller.auth;

import com.aptisprep.dto.form.RegisterFormDTO;
import com.aptisprep.dto.view.AuthResultDTO;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.DuplicateEmailException;
import com.aptisprep.security.LoginSession;
import com.aptisprep.service.account.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Objects;
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
    private final com.aptisprep.service.account.AccountUnlockRequestService
            accountUnlockRequestService;

    public AuthController(UserService userService) {
        this(userService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AuthController(
            UserService userService,
            com.aptisprep.service.account.AccountUnlockRequestService accountUnlockRequestService) {
        this.userService = userService;
        this.accountUnlockRequestService = accountUnlockRequestService;
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
        AuthResultDTO result = userService.login(request, email, password);
        if (result.locked()) {
            redirectAttributes.addFlashAttribute(
                    "email", result.email() != null ? result.email() : email);
            redirectAttributes.addFlashAttribute("status", "LOCKED");
            redirectAttributes.addFlashAttribute(
                    "lockReason",
                    result.lockReason() == null || result.lockReason().isBlank()
                            ? "Tài khoản đã bị khóa bởi quản trị viên."
                            : result.lockReason());
            return "redirect:/account-locked";
        }
        if (result.inactive()) {
            redirectAttributes.addFlashAttribute(
                    "email", result.email() != null ? result.email() : email);
            redirectAttributes.addFlashAttribute("status", "INACTIVE");
            redirectAttributes.addFlashAttribute(
                    "removalReason",
                    result.removalReason() == null || result.removalReason().isBlank()
                            ? "Tài khoản của bạn đã bị ngừng hoạt động."
                            : result.removalReason());
            return "redirect:/account-locked";
        }
        if (!result.success()) {
            redirectAttributes.addFlashAttribute("errorMessage", result.errorMessage());
            return "redirect:/login";
        }
        return "redirect:" + result.redirectUrl();
    }

    @GetMapping("/account-locked")
    public String accountLocked(
            @RequestParam(name = "email", required = false) String paramEmail, Model model) {
        String email = (String) model.getAttribute("email");
        if ((email == null || email.isBlank()) && paramEmail != null) {
            email = paramEmail.trim();
            model.addAttribute("email", email);
        }
        if (email != null && !email.isBlank()) {
            accountUnlockRequestService
                    .findLatestByEmail(email)
                    .ifPresent(req -> model.addAttribute("latestRequest", req));
        }
        return "auth/account-locked";
    }

    @PostMapping({"/account-unlock-request", "/account-locked"})
    public String submitUnlockRequest(
            @RequestParam(name = "email", defaultValue = "") String email,
            @RequestParam(name = "password", defaultValue = "") String password,
            @RequestParam(name = "message", defaultValue = "") String message,
            RedirectAttributes redirectAttributes) {
        String normalizedEmail = email == null ? "" : email.trim();
        try {
            accountUnlockRequestService.submit(normalizedEmail, password, message);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Yêu cầu mở khóa đã được gửi thành công. Vui lòng chờ quản trị viên xem xét.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        redirectAttributes.addFlashAttribute("email", normalizedEmail);
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
            model.addAttribute("registerForm", new RegisterFormDTO());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerForm") RegisterFormDTO form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

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

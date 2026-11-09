package com.aptisprep.controller.auth;

import com.aptisprep.dto.form.RegisterForm;
import com.aptisprep.entity.User;
import com.aptisprep.enums.Role;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.DuplicateEmailException;
import com.aptisprep.security.LoginSession;
import com.aptisprep.service.auth.UserService;
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

    public AuthController(UserService userService) {
        this.userService = userService;
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
        User user = userService.getUserByEmailAndPassword(email, password);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Email hoặc mật khẩu không đúng.");
            return "redirect:/login";
        }
        LoginSession.start(request, user);
        return user.getRole() == Role.ADMIN
                ? "redirect:/admin/dashboard"
                : "redirect:/student/dashboard";
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
            model.addAttribute("registerForm", new RegisterForm());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerForm") RegisterForm form,
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

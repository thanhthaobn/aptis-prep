package com.aptisprep.mvc.auth.service.impl;

import com.aptisprep.mvc.auth.dto.AuthResultDTO;
import com.aptisprep.mvc.auth.dto.RegisterFormDTO;
import com.aptisprep.mvc.auth.dto.UserEntityDTO;
import com.aptisprep.mvc.auth.entity.User;
import com.aptisprep.mvc.auth.repository.UserRepository;
import com.aptisprep.mvc.auth.service.UserService;
import com.aptisprep.mvc.common.enums.Role;
import com.aptisprep.mvc.common.enums.UserStatus;
import com.aptisprep.mvc.common.exception.BusinessRuleException;
import com.aptisprep.mvc.common.exception.DuplicateEmailException;
import com.aptisprep.mvc.common.exception.ResourceNotFoundException;
import com.aptisprep.mvc.common.security.LoginSession;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final String dummyPasswordHash;

    public UserServiceImpl(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.dummyPasswordHash =
                passwordHasher == null ? "" : passwordHasher.encode("unused-login-password");
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByEmailAndPassword(String email, String password) {
        String normalizedEmail = email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
        if (normalizedEmail.isBlank()
                || normalizedEmail.length() > 255
                || password == null
                || password.isEmpty()
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            return null;
        }
        Optional<User> user = userRepository.findByEmailIgnoreCase(normalizedEmail);
        String hash = user.map(User::getPasswordHash).orElse(dummyPasswordHash);
        return passwordHasher != null && passwordHasher.matches(password, hash)
                ? user.orElse(null)
                : null;
    }

    @Override
    @Transactional(readOnly = true)
    public User requireByEmail(String email) {
        return userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
    }

    @Override
    @Transactional
    public User registerStudent(RegisterFormDTO form) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new BusinessRuleException("Mật khẩu xác nhận không khớp");
        }
        String normalizedEmail = form.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateEmailException();
        }
        User user = UserEntityDTO.convertDtoToEntity(form);
        user.setFullName(user.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordHasher.encode(form.getPassword()));
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.saveAndFlush(user);
    }

    @Override
    @Transactional
    public User registerStudent(HttpServletRequest request, RegisterFormDTO form) {
        User user = registerStudent(form);
        LoginSession.start(request, user);
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResultDTO login(HttpServletRequest request, String email, String password) {
        User user = getUserByEmailAndPassword(email, password);
        if (user == null) {
            return AuthResultDTO.failed("Email hoặc mật khẩu không đúng.");
        }
        if (user.getStatus() == UserStatus.LOCKED) {
            String reason = user.getLockReason();
            return AuthResultDTO.locked(
                    user.getEmail(),
                    reason == null || reason.isBlank()
                            ? "Tài khoản đã bị khóa bởi quản trị viên."
                            : reason);
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            String reason = user.getRemovalReason();
            return AuthResultDTO.inactive(
                    user.getEmail(),
                    reason == null || reason.isBlank()
                            ? "Tài khoản của bạn đã bị ngừng hoạt động."
                            : reason);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            return AuthResultDTO.failed("Tài khoản không còn hoạt động.");
        }
        LoginSession.start(request, user);
        return AuthResultDTO.success(
                user.getRole() == Role.ADMIN ? "/admin/dashboard" : "/student/dashboard");
    }

    @Override
    public Optional<User> findById(Integer id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByIdForUpdate(Integer id) {
        return userRepository.findByIdForUpdate(id);
    }

    @Override
    public Optional<User> findByEmailIgnoreCase(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getUserIdByEmail(String email) {
        return requireByEmail(email).getId();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByRole(com.aptisprep.mvc.common.enums.Role role) {
        return userRepository.countByRole(role);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByRoleAndStatus(
            com.aptisprep.mvc.common.enums.Role role,
            com.aptisprep.mvc.common.enums.UserStatus status) {
        return userRepository.countByRoleAndStatus(role, status);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<User> searchStudents(
            String pattern, String status, org.springframework.data.domain.Pageable pageable) {
        return userRepository.searchStudents(pattern, status, pageable);
    }
}

package com.aptisprep.service.auth.impl;

import com.aptisprep.dto.UserEntityDto;
import com.aptisprep.dto.form.RegisterForm;
import com.aptisprep.entity.User;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.DuplicateEmailException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.UserRepository;
import com.aptisprep.service.auth.PasswordHasher;
import com.aptisprep.service.auth.UserService;
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
        this.dummyPasswordHash = passwordHasher.encode("unused-login-password");
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
        return passwordHasher.matches(password, hash) ? user.orElse(null) : null;
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
    public User registerStudent(RegisterForm form) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new BusinessRuleException("Mật khẩu xác nhận không khớp");
        }
        String normalizedEmail = form.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateEmailException();
        }
        User user = UserEntityDto.convertDtoToEntity(form);
        user.setFullName(user.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordHasher.encode(form.getPassword()));
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.saveAndFlush(user);
    }
}

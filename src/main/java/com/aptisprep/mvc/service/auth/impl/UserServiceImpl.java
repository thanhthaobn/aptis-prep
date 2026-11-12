package com.aptisprep.mvc.service.auth.impl;

import com.aptisprep.mvc.dto.*;
import com.aptisprep.mvc.entity.User;
import com.aptisprep.mvc.enums.Role;
import com.aptisprep.mvc.enums.UserStatus;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.exception.DuplicateEmailException;
import com.aptisprep.mvc.repository.UserRepository;
import com.aptisprep.mvc.support.auth.PasswordHasher;
import com.aptisprep.mvc.service.auth.UserService;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {
    private UserRepository userRepository;
    private PasswordHasher passwordHasher;
    private String dummyPasswordHash;

    public UserServiceImpl(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.dummyPasswordHash = passwordHasher.encode("unused-login-password");
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO authenticate(String email, String password) {
        User user = getUserByEmailAndPassword(email, password);
        return toAuthenticatedUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO findAuthenticatedById(Integer userId) {
        return userRepository.findById(userId).map(this::toAuthenticatedUser).orElse(null);
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
        String hash = user.map(streamValue -> streamValue.getPasswordHash()).orElse(dummyPasswordHash);
        return passwordHasher.matches(password, hash) ? user.orElse(null) : null;
    }

    @Override
    @Transactional
    public User registerStudent(UserDTO form) {
        if (form == null
                || form.getPassword() == null
                || form.getConfirmPassword() == null
                || !Objects.equals(form.getPassword(), form.getConfirmPassword())) {
            throw new BusinessRuleException("Mật khẩu xác nhận không khớp");
        }
        if (form.getEmail() == null || form.getEmail().isBlank()) {
            throw new BusinessRuleException("Email không được để trống");
        }
        String normalizedEmail = form.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateEmailException();
        }
        User user = UserDTO.convertDtoToEntity(form);
        user.setFullName(form.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordHasher.encode(form.getPassword()));
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.saveAndFlush(user);
    }

    private UserDTO toAuthenticatedUser(User user) {
        if (user == null) return null;
        return UserDTO.convertEntityToDto(user);
    }
}








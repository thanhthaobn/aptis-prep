package com.aptisprep.service.auth;

import com.aptisprep.dto.form.RegisterForm;
import com.aptisprep.entity.User;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.repository.UserRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerStudent(RegisterForm form) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new BusinessRuleException("Mật khẩu xác nhận không khớp");
        }

        String normalizedEmail = form.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BusinessRuleException("Email đã được sử dụng");
        }

        User user = new User();
        user.setFullName(form.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }
}

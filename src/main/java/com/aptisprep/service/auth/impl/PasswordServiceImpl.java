package com.aptisprep.service.auth.impl;

import com.aptisprep.entity.User;
import com.aptisprep.mvc.common.enums.UserStatus;
import com.aptisprep.mvc.common.exception.BusinessRuleException;
import com.aptisprep.repository.UserRepository;
import com.aptisprep.service.auth.PasswordHasher;
import com.aptisprep.service.auth.PasswordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordServiceImpl implements PasswordService {
    private final UserRepository users;
    private final PasswordHasher encoder;

    public PasswordServiceImpl(UserRepository users, PasswordHasher encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void change(String email, String current, String password) {
        User existing = users.findByEmailIgnoreCase(email).orElseThrow(this::invalid);
        User user = users.findByIdForUpdate(existing.getId()).orElseThrow(this::invalid);
        if (user.getStatus() != UserStatus.ACTIVE
                || current == null
                || !encoder.matches(current, user.getPasswordHash()))
            throw new BusinessRuleException("Mật khẩu hiện tại không đúng");
        savePassword(user, password);
    }

    private void savePassword(User user, String password) {
        if (password == null
                || password.isBlank()
                || password.length() < 8
                || password.length() > 72
                || password.codePoints().noneMatch(Character::isUpperCase)
                || password.codePoints().noneMatch(Character::isDigit)
                || password.codePoints()
                        .noneMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c))
                || password.codePoints().anyMatch(Character::isWhitespace))
            throw new BusinessRuleException(
                    "Mật khẩu phải có từ 8 đến 72 ký tự và không chứa khoảng trắng");
        if (encoder.matches(password, user.getPasswordHash()))
            throw new BusinessRuleException("Mật khẩu mới cần khác mật khẩu hiện tại");
        user.setPasswordHash(encoder.encode(password));
        users.saveAndFlush(user);
    }

    private BusinessRuleException invalid() {
        return new BusinessRuleException("Không tìm thấy tài khoản hoặc tài khoản không hợp lệ");
    }
}

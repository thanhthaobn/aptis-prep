package com.aptisprep.service.auth;

import com.aptisprep.entity.User;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.repository.UserRepository;
import com.aptisprep.validation.AccountInput;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class PasswordService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final ObjectProvider<JavaMailSender> mail;
    private final String secret, baseUrl, from;
    private final Map<Integer, Instant> sent = new ConcurrentHashMap<>();

    public PasswordService(
            UserRepository users,
            PasswordEncoder encoder,
            ObjectProvider<JavaMailSender> mail,
            @Value("${app.password-reset.secret:${PASSWORD_RESET_SECRET:}}") String secret,
            @Value(
                            "${app.password-reset.base-url:${PASSWORD_RESET_BASE_URL:http://localhost:8080}}")
                    String baseUrl,
            @Value("${app.password-reset.from:${PASSWORD_RESET_FROM:}}") String from) {
        this.users = users;
        this.encoder = encoder;
        this.mail = mail;
        this.secret = secret;
        this.baseUrl = baseUrl;
        this.from = from;
    }

    public boolean recoveryAvailable() {
        return secret.length() >= 32 && !from.isBlank() && mail.getIfAvailable() != null;
    }

    public void requestReset(String email) {
        if (!recoveryAvailable())
            throw new BusinessRuleException(
                    "Khôi phục qua email chưa được cấu hình. Vui lòng liên hệ quản trị viên.");
        var found = users.findByEmailIgnoreCase(AccountInput.email(email));
        if (found.isEmpty() || found.get().getStatus() != UserStatus.ACTIVE) return;
        User user = found.get();
        Instant now = Instant.now();
        synchronized (sent) {
            sent.entrySet().removeIf(e -> e.getValue().isBefore(now.minusSeconds(60)));
            if (sent.containsKey(user.getId())) return;
            sent.put(user.getId(), now);
        }
        String payload = user.getId() + "." + now.plusSeconds(900).getEpochSecond();
        String token = payload + "." + signature(payload, user.getPasswordHash());
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject("Đặt lại mật khẩu AptisPrep");
        message.setText(
                "Mở liên kết sau để đặt lại mật khẩu trong 15 phút:\n"
                        + baseUrl.replaceAll("/$", "")
                        + "/reset-password?token="
                        + token
                        + "\nNếu bạn không yêu cầu, hãy bỏ qua email này.");
        try {
            mail.getObject().send(message);
        } catch (org.springframework.mail.MailException exception) {
            // Do not expose account existence, SMTP credentials, or reset tokens in responses/logs.
            org.slf4j.LoggerFactory.getLogger(PasswordService.class)
                    .warn("Password recovery email could not be sent");
        }
    }

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

    @Transactional
    public void reset(String token, String password) {
        String[] parts = token == null ? new String[0] : token.split("\\.");
        if (!recoveryAvailable() || parts.length != 3 || token.length() > 180) throw invalid();
        try {
            int id = Integer.parseInt(parts[0]);
            long expiry = Long.parseLong(parts[1]);
            long now = Instant.now().getEpochSecond();
            if (expiry <= now || expiry > now + 900) throw invalid();
            User user = users.findByIdForUpdate(id).orElseThrow(this::invalid);
            String expected = signature(parts[0] + "." + parts[1], user.getPasswordHash());
            if (user.getStatus() != UserStatus.ACTIVE
                    || !MessageDigest.isEqual(
                            expected.getBytes(StandardCharsets.UTF_8),
                            parts[2].getBytes(StandardCharsets.UTF_8))) throw invalid();
            savePassword(user, password);
        } catch (NumberFormatException exception) {
            throw invalid();
        }
    }

    private void savePassword(User user, String password) {
        if (password == null || password.isBlank() || !AccountInput.strongPassword(password))
            throw new BusinessRuleException(AccountInput.PASSWORD_HINT);
        if (encoder.matches(password, user.getPasswordHash()))
            throw new BusinessRuleException("Mật khẩu mới cần khác mật khẩu hiện tại");
        user.setPasswordHash(encoder.encode(password));
        users.saveAndFlush(user);
    }

    private BusinessRuleException invalid() {
        return new BusinessRuleException(
                "Liên kết không hợp lệ, đã dùng hoặc hết hạn. Vui lòng yêu cầu liên kết mới.");
    }

    private String signature(String payload, String hash) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(
                            mac.doFinal((payload + "." + hash).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }
}

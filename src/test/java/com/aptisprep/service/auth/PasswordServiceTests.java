package com.aptisprep.service.auth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aptisprep.entity.User;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordServiceTests {
    @Test
    void resetTokenRejectsTamperingAndReplayAndThrottlesRepeatedRequests() {
        var users = mock(UserRepository.class);
        var encoder = new BCryptPasswordEncoder(4);
        var sender = mock(JavaMailSender.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        when(provider.getObject()).thenReturn(sender);
        var service =
                new PasswordService(
                        users,
                        encoder,
                        provider,
                        "a-test-secret-with-at-least-32-bytes",
                        "https://aptis.example",
                        "noreply@aptis.example");
        var user = new User();
        user.setId(3);
        user.setEmail("thao@example.com");
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash(encoder.encode("Old-password-2026"));
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(users.findByIdForUpdate(3)).thenReturn(Optional.of(user));
        service.requestReset(user.getEmail());
        service.requestReset(user.getEmail());
        var message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender, times(1)).send(message.capture());
        String token = message.getValue().getText().split("token=")[1].split("\\n")[0];
        assertThatThrownBy(() -> service.reset(token + "bad", "New-password-2026"))
                .hasMessageContaining("Liên kết không hợp lệ");
        String[] parts = token.split("\\.");
        assertThatThrownBy(() -> service.reset(parts[0] + ".1." + parts[2], "New-password-2026"))
                .hasMessageContaining("hết hạn");
        service.reset(token, "New-password-2026");
        assertThat(encoder.matches("New-password-2026", user.getPasswordHash())).isTrue();
        assertThatThrownBy(() -> service.reset(token, "Another-password-2026"))
                .hasMessageContaining("đã dùng");
    }
}

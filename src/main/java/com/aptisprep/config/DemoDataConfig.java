package com.aptisprep.config;

import com.aptisprep.enums.Role;
import com.aptisprep.service.auth.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataConfig {

    @Bean
    ApplicationRunner seedDemoUsers(UserService userService,
            @Value("${app.demo.admin-email:admin@aptis.local}") String adminEmail,
            @Value("${app.demo.admin-password:}") String adminPassword,
            @Value("${app.demo.student-email:student@aptis.local}") String studentEmail,
            @Value("${app.demo.student-password:}") String studentPassword) {
        return arguments -> {
            if (adminPassword.isBlank() || studentPassword.isBlank()) {
                throw new IllegalStateException("Demo data is enabled but demo passwords are not configured");
            }
            userService.createDemoUserIfMissing(
                    "System Admin", adminEmail, adminPassword, Role.ADMIN);
            userService.createDemoUserIfMissing(
                    "Demo Student", studentEmail, studentPassword, Role.STUDENT);
        };
    }
}

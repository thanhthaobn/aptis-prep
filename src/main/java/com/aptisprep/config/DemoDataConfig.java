package com.aptisprep.config;

import com.aptisprep.enums.Role;
import com.aptisprep.service.auth.UserService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class DemoDataConfig {

    @Bean
    ApplicationRunner seedDemoUsers(UserService userService) {
        return arguments -> {
            userService.createDemoUserIfMissing(
                    "System Admin", "admin@aptis.local", "Password@123", Role.ADMIN);
            userService.createDemoUserIfMissing(
                    "Demo Student", "student@aptis.local", "Password@123", Role.STUDENT);
        };
    }
}

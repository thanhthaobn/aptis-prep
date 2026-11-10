package com.aptisprep.mvc.auth.service.impl;

import at.favre.lib.crypto.bcrypt.BCrypt;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

@Component
public class PasswordHasher {
    public String encode(String password) {
        return BCrypt.withDefaults().hashToString(10, password.toCharArray());
    }

    public boolean matches(String password, String hash) {
        return password.getBytes(StandardCharsets.UTF_8).length <= 72
                && BCrypt.verifyer().verify(password.toCharArray(), hash).verified;
    }
}


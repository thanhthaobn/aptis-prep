package com.aptisprep.service.account;

import static org.assertj.core.api.Assertions.*;

import com.aptisprep.service.account.impl.PasswordHasher;
import org.junit.jupiter.api.Test;

class PasswordServiceTests {
    @Test
    void verifiesExistingBcryptHashesAtTheUtf8LengthLimit() {
        // Produced by the former encoder from a synthetic 72-byte password.
        String legacyHash = "$2a$04$w8S7HTjIfG.8RRVOhLZWtuRZnMfgZfzBVgmS6DM5RvNYiWFDoZaFi";
        String password = "á".repeat(34) + "Aa1!";
        var hasher = new PasswordHasher();
        assertThat(hasher.matches(password, legacyHash)).isTrue();
        assertThat(hasher.matches(password + "x", legacyHash)).isFalse();
        assertThat(hasher.matches("wrong-password", legacyHash)).isFalse();
    }
}

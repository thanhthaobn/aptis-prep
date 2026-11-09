package com.aptisprep.service.auth;

public interface PasswordService {
    boolean recoveryAvailable();

    void requestReset(String email);

    void change(String email, String current, String password);

    void reset(String token, String password);
}

package com.aptisprep.service.auth;

public interface PasswordService {
    void change(String email, String current, String password);
}

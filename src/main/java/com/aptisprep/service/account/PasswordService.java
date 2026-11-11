package com.aptisprep.service.account;

public interface PasswordService {
    void change(String email, String current, String password);
}

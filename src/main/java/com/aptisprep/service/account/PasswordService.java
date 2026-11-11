package com.aptisprep.service.account;

public interface PasswordService {
    public void change(String email, String current, String password);
}

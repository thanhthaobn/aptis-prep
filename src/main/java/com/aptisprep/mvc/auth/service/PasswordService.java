package com.aptisprep.mvc.auth.service;

public interface PasswordService {
    void change(String email, String current, String password);
}


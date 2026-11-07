package com.aptisprep.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aptisprep.controller.auth.AuthController;
import com.aptisprep.dto.form.RegisterForm;
import com.aptisprep.service.auth.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class RegistrationRaceTests {
    @Test
    void simultaneousDuplicateIsReturnedAsAnEmailFieldError() {
        var service = mock(UserService.class);
        var controller = new AuthController(service);
        var form = new RegisterForm();
        form.setFullName("Thảo");
        form.setEmail("thao@example.com");
        form.setPassword("Learning-English-2026");
        form.setConfirmPassword(form.getPassword());
        var errors = new BeanPropertyBindingResult(form, "registerForm");
        when(service.registerStudent(form))
                .thenThrow(new DataIntegrityViolationException("duplicate email"));
        assertThat(controller.register(form, errors, new RedirectAttributesModelMap()))
                .isEqualTo("auth/register");
        assertThat(errors.hasFieldErrors("email")).isTrue();
        assertThat(form.getEmail()).isEqualTo("thao@example.com");
        assertThat(form.getFullName()).isEqualTo("Thảo");
    }
}

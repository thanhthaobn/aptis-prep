package com.aptisprep.controller;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.RequestContext;

/** Presentation formatting shared by HTML pages; does not query or modify business data. */
@Component
public class DisplayFormatting {
    public boolean fieldInvalid(String formName, String field) {
        if (!(RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes)) return false;
        Errors errors = new RequestContext(attributes.getRequest()).getErrors(formName, false);
        return errors != null && errors.hasFieldErrors(field);
    }

    public List<String> globalErrors(String formName) {
        return formErrors(formName, true);
    }

    public List<String> allErrors(String formName) {
        return formErrors(formName, false);
    }

    private List<String> formErrors(String formName, boolean globalOnly) {
        if (!(RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes)) {
            return List.of();
        }
        RequestContext context = new RequestContext(attributes.getRequest());
        Errors errors = context.getErrors(formName, false);
        if (errors == null) return List.of();
        List<String> messages = new ArrayList<>();
        List<ObjectError> selected = globalOnly ? errors.getGlobalErrors() : errors.getAllErrors();
        for (ObjectError error : selected) {
            messages.add(context.getMessage(error, false));
        }
        return messages;
    }

}

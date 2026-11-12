package com.aptisprep;

/**
 * Backward-compatible launcher for IDE run configurations that still reference
 * the pre-MVC application class name. The actual Spring Boot configuration lives
 * in {@code com.aptisprep.mvc.AptisPrepApplication}.
 */
public final class AptisPrepApplication {

    private AptisPrepApplication() {}

    public static void main(String[] args) {
        com.aptisprep.mvc.AptisPrepApplication.main(args);
    }
}

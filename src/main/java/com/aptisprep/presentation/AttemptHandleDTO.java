package com.aptisprep.presentation;

import java.time.Instant;

public record AttemptHandleDTO(Integer attemptId, String state, Instant deadline) {}

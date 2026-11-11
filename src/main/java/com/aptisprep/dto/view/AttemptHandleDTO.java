package com.aptisprep.dto.view;

import java.time.Instant;

public record AttemptHandleDTO(Integer attemptId, String state, Instant deadline) {}

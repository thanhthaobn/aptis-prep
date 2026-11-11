package com.aptisprep.mvc.exam.dto;

import java.time.Instant;

public record AttemptHandleDTO(Integer attemptId, String state, Instant deadline) {}

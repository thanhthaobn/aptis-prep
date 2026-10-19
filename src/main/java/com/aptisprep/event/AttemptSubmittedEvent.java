package com.aptisprep.event;

public record AttemptSubmittedEvent(Integer userId, Integer attemptId, Integer examId, String examType) {}

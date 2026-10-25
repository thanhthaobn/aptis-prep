package com.aptisprep.event;

public record AttemptCompletedEvent(Integer userId, Integer attemptId, String examType) {}

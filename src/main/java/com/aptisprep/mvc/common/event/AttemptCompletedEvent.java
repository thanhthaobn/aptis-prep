package com.aptisprep.mvc.common.event;

public record AttemptCompletedEvent(Integer userId, Integer attemptId, String examType) {}

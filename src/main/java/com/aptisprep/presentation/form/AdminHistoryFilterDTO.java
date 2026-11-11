package com.aptisprep.presentation.form;

import java.time.Instant;

public record AdminHistoryFilterDTO(
        String query,
        String skill,
        String examType,
        String state,
        Instant submittedFrom,
        Instant submittedTo) {}

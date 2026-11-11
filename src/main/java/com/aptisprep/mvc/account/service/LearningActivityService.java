package com.aptisprep.mvc.account.service;

import com.aptisprep.mvc.account.dto.LearningActivityDTO.ActivityViewDTO;

public interface LearningActivityService {
    ActivityViewDTO activity(String email);
}

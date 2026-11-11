package com.aptisprep.service.account;

import com.aptisprep.dto.LearningActivityDTO.ActivityViewDTO;

public interface LearningActivityService {
    public ActivityViewDTO activity(String email);
}

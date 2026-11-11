package com.aptisprep.service.account;

import com.aptisprep.presentation.LearningActivityDTO.ActivityViewDTO;

public interface LearningActivityService {
    ActivityViewDTO activity(String email);
}

package com.aptisprep.service.account;

import com.aptisprep.dto.view.LearningActivityDTO.ActivityViewDTO;

public interface LearningActivityService {
    ActivityViewDTO activity(String email);
}

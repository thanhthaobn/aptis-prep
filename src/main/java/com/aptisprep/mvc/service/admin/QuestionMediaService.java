package com.aptisprep.mvc.service.admin;

import org.springframework.web.multipart.MultipartFile;

/** Stores and validates media attached to an authored question. */
public interface QuestionMediaService {
    public void save(Integer questionId, MultipartFile file);
}

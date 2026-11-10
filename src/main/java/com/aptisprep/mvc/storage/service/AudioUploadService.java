package com.aptisprep.mvc.storage.service;

import org.springframework.web.multipart.MultipartFile;

public interface AudioUploadService {
    String store(MultipartFile file, Integer attemptId);
}

package com.aptisprep.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface AudioUploadService {
    public String store(MultipartFile file, Integer attemptId);
}

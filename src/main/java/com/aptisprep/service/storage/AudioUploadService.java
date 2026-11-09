package com.aptisprep.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface AudioUploadService {
    String store(MultipartFile file, Integer attemptId);
}

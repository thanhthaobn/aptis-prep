package com.aptisprep.service.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Path;

public interface StorageService {

    String store(MultipartFile file, String folder);

    Resource load(String relativePath);

    void delete(String relativePath);

    default Path localPath(String relativePath) {
        try {
            return load(relativePath).getFile().toPath();
        } catch (IOException | UnsupportedOperationException exception) {
            throw new IllegalStateException("File lưu trữ không hỗ trợ truy cập local: " + relativePath, exception);
        }
    }
}

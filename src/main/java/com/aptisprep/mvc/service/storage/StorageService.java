package com.aptisprep.mvc.service.storage;

import java.io.IOException;
import java.nio.file.Path;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    public String store(MultipartFile file, String folder);

    public Resource load(String relativePath);

    public void delete(String relativePath);

    public default Path localPath(String relativePath) {
        try {
            return load(relativePath).getFile().toPath();
        } catch (IOException | UnsupportedOperationException exception) {
            throw new IllegalStateException(
                    "File lưu trữ không hỗ trợ truy cập local: " + relativePath, exception);
        }
    }
}








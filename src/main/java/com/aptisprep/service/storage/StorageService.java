package com.aptisprep.service.storage;

import java.nio.file.Path;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    public String store(MultipartFile file, String folder);

    public Resource load(String relativePath);

    public void delete(String relativePath);

    public Path localPath(String relativePath);
}

package com.aptisprep.mvc.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private Path uploadDir = Path.of("uploads");

    public Path getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(Path uploadDir) {
        this.uploadDir = uploadDir;
    }
}

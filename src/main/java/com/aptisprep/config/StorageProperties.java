package com.aptisprep.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

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

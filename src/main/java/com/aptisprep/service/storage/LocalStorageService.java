package com.aptisprep.service.storage;

import com.aptisprep.config.StorageProperties;
import com.aptisprep.exception.BusinessRuleException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.Locale;
import java.util.Set;

@Service
public class LocalStorageService implements StorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".pdf", ".mp3", ".wav", ".webm", ".m4a", ".mp4", ".ogg");
    private static final long MAX_BYTES = 50L * 1024 * 1024;

    private final Path root;

    public LocalStorageService(StorageProperties storageProperties) {
        this.root = storageProperties.getUploadDir().toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("File tải lên không được để trống");
        }

        String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String extension = originalName.lastIndexOf('.') >= 0
                ? originalName.substring(originalName.lastIndexOf('.')) : "";
        extension = extension.toLowerCase(Locale.ROOT);
        // Baseline guard only. Callers must also validate actual bytes/MIME and purpose.
        if (!ALLOWED_EXTENSIONS.contains(extension) || file.getSize() > MAX_BYTES) {
            throw new BusinessRuleException("Định dạng hoặc dung lượng file không hợp lệ");
        }
        String storedName = UUID.randomUUID() + extension;

        Path targetDirectory = safeResolve(folder);
        Path target = targetDirectory.resolve(storedName).normalize();
        if (!target.startsWith(targetDirectory)) {
            throw new BusinessRuleException("Đường dẫn lưu file không hợp lệ");
        }

        try {
            Files.createDirectories(targetDirectory);
            try (var input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return root.relativize(target).toString().replace('\\', '/');
        } catch (IOException exception) {
            throw new BusinessRuleException("Không thể lưu file tải lên");
        }
    }

    @Override
    public Resource load(String relativePath) {
        Path target = safeResolve(relativePath);
        try {
            Resource resource = new UrlResource(target.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessRuleException("Không tìm thấy file");
            }
            return resource;
        } catch (MalformedURLException exception) {
            throw new BusinessRuleException("Đường dẫn file không hợp lệ");
        }
    }

    @Override
    public void delete(String relativePath) {
        try {
            Files.deleteIfExists(safeResolve(relativePath));
        } catch (IOException exception) {
            throw new BusinessRuleException("Không thể xóa file");
        }
    }

    private Path safeResolve(String relativePath) {
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new BusinessRuleException("Đường dẫn file không hợp lệ");
        }
        return resolved;
    }
}

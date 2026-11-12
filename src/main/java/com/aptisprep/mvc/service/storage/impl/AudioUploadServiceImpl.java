package com.aptisprep.mvc.service.storage.impl;

import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.service.storage.AudioUploadService;
import com.aptisprep.mvc.service.storage.StorageService;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Validates learner audio before it is persisted; clients never choose a filesystem path. */
@Service
public class AudioUploadServiceImpl implements AudioUploadService {
    private long MAX_AUDIO_BYTES = 25L * 1024 * 1024;
    private Set<String> EXTENSIONS =
            Set.of(".mp3", ".wav", ".webm", ".m4a", ".mp4", ".ogg", ".flac");
    private StorageService storage;

    public AudioUploadServiceImpl(StorageService storage) {
        this.storage = storage;
    }

    public String store(MultipartFile file, Integer attemptId) {
        validate(file);
        return storage.store(file, "attempts/" + attemptId + "/audio");
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new BusinessRuleException("File ghi âm không được để trống");
        if (file.getSize() > MAX_AUDIO_BYTES)
            throw new BusinessRuleException("File ghi âm không được vượt quá 25MB");
        String name =
                file.getOriginalFilename() == null
                        ? ""
                        : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot);
        String contentType =
                file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!EXTENSIONS.contains(extension)
                || !(contentType.startsWith("audio/") || "video/mp4".equals(contentType))) {
            throw new BusinessRuleException("Chỉ chấp nhận file ghi âm hợp lệ");
        }
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(16);
            if (!matchesAudioSignature(header))
                throw new BusinessRuleException("Nội dung file không phải audio hợp lệ");
        } catch (IOException exception) {
            throw new BusinessRuleException("Không thể đọc file ghi âm");
        }
    }

    private boolean matchesAudioSignature(byte[] bytes) {
        if (bytes.length >= 12 && ascii(bytes, 0, "RIFF") && ascii(bytes, 8, "WAVE")) return true;
        if (bytes.length >= 4 && ascii(bytes, 0, "OggS")) return true;
        if (bytes.length >= 3 && ascii(bytes, 0, "ID3")) return true;
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xE0) == 0xE0)
            return true;
        if (bytes.length >= 4
                && (bytes[0] & 0xFF) == 0x1A
                && (bytes[1] & 0xFF) == 0x45
                && (bytes[2] & 0xFF) == 0xDF
                && (bytes[3] & 0xFF) == 0xA3) return true;
        return bytes.length >= 8 && ascii(bytes, 4, "ftyp");
    }

    private boolean ascii(byte[] bytes, int offset, String value) {
        if (bytes.length < offset + value.length()) return false;
        for (int i = 0; i < value.length(); i++)
            if (bytes[offset + i] != (byte) value.charAt(i)) return false;
        return true;
    }
}
